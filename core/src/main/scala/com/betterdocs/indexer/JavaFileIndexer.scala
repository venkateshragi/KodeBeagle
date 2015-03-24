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

package com.betterdocs.indexer

import java.util.regex.Pattern

import com.betterdocs.configuration.BetterDocsConfig
import com.betterdocs.crawler.Repository
import com.betterdocs.parser.MethodVisitor

import scala.collection.mutable
import scala.collection.immutable
import scala.util.Try


case class IndexEntry(repoId: Int, file: String, tokens: Set[Token], score: Int)

/* Since our tokens are fully qualified import names. */
case class Token(importName: String, lineNumbers: immutable.SortedSet[Int])

trait BasicIndexer extends Serializable {

  /** Adjusting the lines of context is crucial to the kinds of token generated. (Tune this.) */
  def linesOfContext: Int

  /** Import pattern of some languages is similar. */
  val importPattern: Pattern = Pattern.compile("import (.*)\\.(\\w+);")

  def generateTokens(files: Map[String, String], excludePackages: List[String],
      repo: Option[Repository]): Set[IndexEntry]

  protected def mapToTokens(map: mutable.Map[String, immutable.SortedSet[Int]]): Set[Token] = {
    map.map{ case (key, value) => Token(key, value) }.toSet
  }
}

class JavaFileIndexer extends BasicIndexer {

  /** For Java code based on trial and error 10 to 20 seems good. */
  override def linesOfContext: Int = BetterDocsConfig.linesOfContext.toInt

  def fileNameToURL(repo: Repository, f: String) = {
    val (_, actualFileName) = f.splitAt(f.indexOf('/'))
    s"""${repo.login}/${repo.name}/blob/${repo.defaultBranch}$actualFileName"""
  }

  override def generateTokens(files: Map[String, String], excludePackages: List[String],
    repo: Option[Repository]): Set[IndexEntry] = {
    var indexEntries = immutable.HashSet[IndexEntry]()
    val r = repo.getOrElse(Repository.invalid)
    for (file <- files) {
      val (fileName, fileContent) = file
      val tokenMap = new mutable.HashMap[String, immutable.SortedSet[Int]]
      val imports = extractImports(fileContent, excludePackages)
      val fullGithubURL = fileNameToURL(r, fileName)
      // Penalize score of test files.
      val score = if (isTestFile(imports)) r.stargazersCount / 5 else r.stargazersCount
      extractTokensWRTImports(imports, fileContent).foreach { x =>
       x.map { y =>
          val FQCN = tuple2ToImportString(y._2)
          val oldValue: immutable.SortedSet[Int] = tokenMap.getOrElse(FQCN, immutable.SortedSet())
          // This map can be used to create one to N index if required.
          tokenMap += ((FQCN, oldValue ++ List(y._1)))
        }
        indexEntries = indexEntries + IndexEntry(r.id, fullGithubURL, mapToTokens(tokenMap), score)
        //  tokens = tokens ++ List(Token(fullGithubURL, x.map(z => z._2._1 + "." + z._2
        //  ._2), x.head._1, score))
        tokenMap.clear()
      }

      Try(extractTokensASTParser(imports, fileContent, fileName)).toOption.foreach { x =>
        x.foreach { y => indexEntries = indexEntries + IndexEntry(r.id, fullGithubURL, y, score) }
      }

    }
    indexEntries
  }

  private def tuple2ToImportString(importName: (String, String)): String = {
    importName._1 + "." + importName._2
  }

  private def isTestFile(imports: Set[(String, String)]) = {
    imports.exists(x => x._1.equalsIgnoreCase("org.junit"))
  }

  private def extractTokensASTParser(imports: Set[(String, String)],
      fileContent: String, fileName: String): Set[Set[Token]] = {
    val parser = new MethodVisitor()
    parser.parse(fileContent, fileName)
    val importsSet = imports.map(tuple2ToImportString)
    import scala.collection.JavaConversions._
    parser.getListOflineNumbersMap.map(x => x.map(y => Token(y._1, immutable.SortedSet[Int]() ++
      y._2.map(_.toInt)))
      .filter(x => importsSet.contains(x.importName)).toSet).toSet
  }

  private def extractImports(java: String, packages: List[String]) = java.split("\n")
    .filter(x => x.startsWith("import"))
    .map(x => importPattern.matcher(x)).filter(_.find)
    .flatMap(x => Try((x.group(1),  x.group(2).trim)).toOption)
    .filterNot { case (left, right) => packages.contains(left) }.toSet

  /**
   * Takes a line of code and cleans it for further indexing.
   */
  private def cleanUpCode(line: String): String = {
   val cleaned =
     line.replaceFirst("""(\s*(import|private|public|protected|\/?\*|//).*)|\".*\"""", "")
      .replaceAll("\\W+", " ")
    if(!cleaned.isEmpty) " " + cleaned + " " else ""
  }

  private def extractTokensWRTImports(imports: Set[(String, String)],
      java: String): List[Array[(Int, (String, String))]] = {
    val lines = java.split("\n").map(cleanUpCode)
    (lines.sliding(linesOfContext) zip (1 to lines.length).sliding(linesOfContext)).toList
      .map { case (linesWindow, lineNumbersWindow) =>
      (linesWindow zip lineNumbersWindow).flatMap { case (line, lineNumber) if !line.isEmpty =>
        val l = line.split(" ").distinct
        imports.map(y => (l.contains(y._2), lineNumber, y))
          .filter(_._1).map(a => (a._2, a._3))
      case _ => Set[(Int, (String, String))]()
      }
    }.distinct.filter(_.nonEmpty)
  }
}
