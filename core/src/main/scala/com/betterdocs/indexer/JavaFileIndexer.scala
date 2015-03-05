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

import scala.collection.mutable
import scala.util.Try


/** This is the smallest and the only entity that we store as index. (Read about: Reverse Index.) */
case class Token(file: String, strings: Set[String], lineNumbers: Seq[Int], score: Int)

trait BasicIndexer extends Serializable {

  /** Adjusting the lines of context is crucial to the kinds of token generated. (Tune this.) */
  val linesOfContext: Int

  /** Import pattern of some languages is similar. */
  val importPattern: Pattern = Pattern.compile("import (.*)\\.(\\w+);")

  def generateTokens(files: Map[String, String], excludePackages: List[String], score: Int,
      orgsName: String): List[Token]

}

class JavaFileIndexer extends BasicIndexer {

  /** For Java code based on trial and error 10 to 20 seems good. */
  override val linesOfContext: Int = 10

  override def generateTokens(files: Map[String, String], excludePackages: List[String],
      score: Int, orgsName: String): List[Token] = {
    var tokens = List[Token]()
    for (file <- files) {
      val (fileName, fileContent) = file
      val tokenMap = new mutable.HashMap[String, List[Int]]
      val imports = extractImports(fileContent, excludePackages)
      generateTokensWRTImports(imports, fileContent).map { x =>
       x.map { y =>
          val FQCN = y._2._1 + "." + y._2._2
          val oldValue: List[Int] = tokenMap.getOrElse(FQCN, List())
          // This map can be used to create one to N index if required.
          tokenMap += ((FQCN, oldValue ++ List(y._1)))
        }
        val (repoName, actualFileName) = fileName.splitAt(fileName.indexOf('/'))
        val (actualRepoName, branchName) = repoName.splitAt(fileName.indexOf('-'))
          val fullGithubURL = s"""http://github.com/$orgsName/$actualRepoName/blob/${branchName
          .stripPrefix("-")}$actualFileName"""
        tokens = tokens ++ List(Token(fullGithubURL, tokenMap.keySet.toSet,
          tokenMap.values.flatten.toSeq.sorted.distinct, score))
        //  tokens = tokens ++ List(Token(fullGithubURL, x.map(z => z._2._1 + "." + z._2
        //  ._2), x.head._1, score))
        tokenMap.clear()
      }
    }
    tokens.distinct
  }

  private def extractImports(java: String, packages: List[String]) = java.split("\n")
    .filter(x => x.startsWith("import"))
    .map(x => importPattern.matcher(x)).filter(_.find)
    .flatMap(x => Try(x.group(1) -> x.group(2).trim).toOption)
    .filterNot { case (left, right) => packages.contains(left + "." + right) }

  /**
   * Takes a line of code and cleans it for further indexing.
   */
  private def cleanUpCode(line: String): String = {
    " " + line.replaceFirst("//.*", " ").replaceFirst("import.*", " ")
      .replaceAll("\\W+", " ")
  }

  private def generateTokensWRTImports(imports: Seq[(String, String)],
      java: String): List[Array[(Int, (String, String))]] = {
    val lines = java.split("\n")
    (lines.sliding(linesOfContext) zip (1 to lines.size).sliding(linesOfContext)).toList
      .map { case (linesWindow, lineNumbersWindow) =>
      (linesWindow zip lineNumbersWindow).flatMap { case (line, lineNumber) =>
        imports.map(y => (cleanUpCode(line).contains(" " + y._2 + " "), lineNumber, y))
          .filter(_._1).map(a => (a._2, a._3))
      }
    }.distinct.filter(_.nonEmpty)
  }
}
