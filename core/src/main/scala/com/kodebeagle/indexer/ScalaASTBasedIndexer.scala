/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kodebeagle.indexer

import java.lang.Exception
import java.util.regex.Pattern

import com.kodebeagle.logging.Logger
import com.kodebeagle.parser.ScalaParserUtils
import org.scalastyle.{Lines, Checker}

import scala.collection.immutable.Iterable
import scala.util.Try
import scalariform.parser.FunDefOrDcl

object ScalaASTBasedIndexer extends Logger {

  val importPattern: Pattern = Pattern.compile("import (.*)\\.(\\w+)")

  private def convertToSimpleImports(importClause: String) = {
    val indexOfOpenCurly = importClause.indexOf('{')
    val indexOfClosedCurly = importClause.indexOf('}')
    if (indexOfOpenCurly != -1 && indexOfClosedCurly != -1) {
      val importPrefix = importClause.substring(0, indexOfOpenCurly).trim
      val importSuffixes = importClause.substring(indexOfOpenCurly + 1, indexOfClosedCurly)
        .split(',').filterNot(_.contains("=>"))
      importSuffixes.map(importSuffix => importPrefix ++ importSuffix.trim)
    } else {
      Array[String]()
    }
  }

  def extractImports(source: String, packages: List[String]): Set[(String, String)] = {
    val importLines = source.split("\n").filter(line => line.trim.startsWith("import") &&
      !line.trim.endsWith("._"))
    val groupedImports = importLines.filter(_.contains('{'))
    val absoluteImports = importLines.filterNot(_.contains('{'))
    val allImports = absoluteImports ++ groupedImports.flatMap(convertToSimpleImports)
    allImports.map(importPattern.matcher(_)).filter(_.find)
      .flatMap(x => Try((x.group(1).trim, x.group(2).trim)).toOption)
      .filterNot { case (left, right) => packages.contains(left) }.toSet
  }

  /**
    * Takes a line of code and cleans it for further indexing.
    */
  def cleanUpCode(line: String): String = {
    val cleaned =
      line.replaceFirst("""(\s*(import|private|public|protected|\/?\*|//).*)|\".*\"""", "")
        .replaceAll("\\W+", " ")
    if (!cleaned.isEmpty) " " + cleaned.toLowerCase + " " else ""
  }

  def tuple2ToImportString(importName: (String, String)): String = {
    importName._1 + "." + importName._2
  }

  // This is simplest and considers the whole file a single context in KodeBeagle sense.
  def generateTokens(files: Map[String, String], excludePackages: List[String],
                     repo: Option[Repository]): Set[ImportsMethods] = {
    files.map { case (fileName, fileContent) =>
      val imports: Set[(String, String)] = extractImports(fileContent, excludePackages)
      val lines: Array[String] = fileContent.split("\n")
      val tokens: Array[MethodToken] =
        (lines zip (1 to lines.length)).flatMap { case (line, lineNumber) =>
          val cleaned = cleanUpCode(line)
          val l = cleaned.split("\\s+").distinct
          val importVsLineNumber: Map[String, Set[Int]] =
            imports.map { case y@(importLeft, importRight) =>
              (l.contains(importRight.toLowerCase), lineNumber, y)
            }.filter { case (isMatched, _, _) => true }
              .map { case (_, lineNum, importPair) => (tuple2ToImportString(importPair), lineNum) }
              .groupBy { case (importName, lineNum) => importName }
              .map { case (importName, lineNumSet) => importName -> lineNumSet.map(x1 => x1._2) }

          val methodTokens: Iterable[MethodToken] =
            importVsLineNumber.map { case (importName, lineNumSet) =>
              MethodToken(importName.toLowerCase, importName,
                lineNumSet.map(x => HighLighter(x, 0, line.length)).toList,
                Set())
            }

          methodTokens
        }
      val repository: Repository = repo.getOrElse(Repository.invalid)
      ImportsMethods(repository.id, fileName, tokens.toSet[MethodToken],
        repository.stargazersCount)
    }.filter(_.tokens.nonEmpty).toSet
  }

  def generateTokensWithFunctionContext(files: Map[String, String], excludePackages: List[String],
                                        repo: Option[Repository]): Set[ImportsMethods] = {
    log.info("excluded:" + excludePackages.mkString("~") + "#")
    files.flatMap { case (fileName, fileContent) =>
      val imports: Set[(String, String)] = extractImports(fileContent, excludePackages)
      log.info("imports:" + imports.mkString("~") + "#")
      log.info(s"FileName>>> $fileName")
      val mayBeLines = Try(Checker.parseLines(fileContent))
      if(mayBeLines.isSuccess) {
        val lines = mayBeLines.get
        val functions: List[FunDefOrDcl] = ScalaParserUtils.extractFunctions(fileContent)
        functions.map { function =>
          val allMethodCallExprs = ScalaParserUtils.getAllCallExprs(function)
          val params = ScalaParserUtils.getParams(function)
            .filter(x => imports.map(_._2).contains(x._2))
          // Filter is to remove those param types which are not explicitly imported
          val methodTokens: List[MethodToken] = params.map { param =>
            val listOfImportsAndLines =
              ScalaParserUtils.getImportsLines(function, param._1, lines)
            val listOfMethodAndLines =
              ScalaParserUtils.getCallExprAndLines(allMethodCallExprs, param._1, lines)
            val fqImportName = getImportName(imports, param._2)
            MethodToken(fqImportName.toLowerCase, fqImportName,
              listOfImportsAndLines,
              listOfMethodAndLines.toSet)
          }.filter(x => x.importName.nonEmpty && x.lineNumbers.nonEmpty &&
            x.methodAndLineNumbers.nonEmpty)

          val repository: Repository = repo.getOrElse(Repository.invalid)
          val absoluteFileName: String = JavaFileIndexerHelper.fileNameToURL(repository, fileName)
          ImportsMethods(repository.id, absoluteFileName,
            methodTokens.toSet[MethodToken], repository.stargazersCount)
        }
      } else {
        Set(ImportsMethods(-1, "", Set[MethodToken](), -1))
      }
    }.filter(_.tokens.nonEmpty).toSet
  }

  def getImportName(imports: Set[(String, String)], className: String): String = {
    imports.find(_._2 == className) match {
      case Some(x) => tuple2ToImportString(x)
      case None => ""
    }
  }
}
