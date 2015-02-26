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

import org.apache.commons.lang3.StringUtils

import scala.collection.mutable
import scala.util.Try

case class Token(file: String, string: String, occurs: List[Int])

trait BasicIndexer extends Serializable {

  val linesOfContext: Int = 3
  // Import pattern of some languages is similar.
  val importPattern: Pattern = Pattern.compile("import (.*)\\.(\\w+);")

  def generateTokens(files: Map[String, String], excludePackages: List[String]): List[Token]

}

class JavaFileIndexer extends BasicIndexer {

  override def generateTokens(files: Map[String, String],
      excludePackages: List[String]): List[Token] = {
    var tokens = List[Token]()
    for (file <- files) {
      val tokenMap = new mutable.HashMap[String, List[Int]]
      val imports = extractImports(file._2, excludePackages)
      generateTokensWRTImports(imports, file._2).map { x =>
        x.map { y =>
          val oldValue: List[Int] = tokenMap.getOrElse(y._3._1 + "." + y._3._2, List())
          tokenMap += ((y._3._1 + "." + y._3._2, oldValue ++ List(y._2)))
        }
      }
      tokens = tokens ++ tokenMap.map(x => Token(file._1, x._1, x._2)).toList
    }
    tokens
  }

  private def extractImports(java: String, packages: List[String]) = java.split("\n")
    .filter(x => x.startsWith("import") && !packages.exists(x.contains(_)))
    .map(x => importPattern.matcher(x)).filter(_.find)
    .map(x => Try(x.group(1) -> x.group(2).trim).toOption).flatten

  private def generateTokensWRTImports(imports: Seq[(String, String)],
      java: String): List[Seq[(Int, Int, (String, String))]] = {
    val lines = java.split("\n")
    (lines.sliding(linesOfContext).toList zip (0 to lines.size).toList).map { x =>
      imports.map(y => (StringUtils.countMatches(x._1.mkString("\n"), y._2), x._2, y))
    }.map(_.filter(_._1 > 0)).filter(_.nonEmpty)
  }
}