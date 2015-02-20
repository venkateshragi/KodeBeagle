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

package com.betterdocs.crawler

import java.io.BufferedInputStream
import java.util.regex.Pattern

import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipFile}
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try


/**
 * Parses java files from the given zip file.
 */
object ZipBasicParser {

  private val importPattern: Pattern = Pattern.compile("import (.*)\\.(\\w+);")
  // Determines the lines of context.
  private val linesOfContext: Int = 3

  private val bufferSize = 102400

  def readJavaFiles(zip: ZipFile) = {
    val list = mutable.ArrayBuffer[(String, String)]()
    val zipArchiveEntries = zip.getEntries.toList
    val allJavaFiles = zipArchiveEntries.filter(x => x.getName.endsWith("java") && !x.isDirectory)
    val allPackages =
      zipArchiveEntries.filter(x => !x.getName.endsWith("java") && x.isDirectory).map(_.getName.split("\\/").last).
        filter(x => x != "org" || x != "com" || x != "edu" || x != "apache")
    println(allPackages.mkString("\n"))
    val files = allJavaFiles.map(x => x.getName -> new BufferedInputStream(zip.getInputStream(x)))
    for ((name, f) <- files) {
      val b = new Array[Byte](bufferSize)
      f.read(b)
      list += (name -> new String(b).trim)
    }
    (list, List())
  }

  def extractImports(java: String, packages: List[String]) = java.split("\n")
    .filter(x => x.startsWith("import") && !packages.exists(x.contains(_)))
    .map(x => importPattern.matcher(x)).filter(_.find)
    .map(x => Try(x.group(1) -> x.group(2).trim).toOption).flatten

  def generateTokensWRTImports(imports: Seq[(String, String)], java: String) = {
    val lines = java.split("\n")
    (lines.sliding(linesOfContext) zip (0 to lines.size).toIterator).map { x =>
      imports.map(y => (StringUtils.countMatches(x._1.mkString("\n"), y._2), x._2, y))
    }.map(_.filter(_._1 > 0)).filter(_.nonEmpty)
  }

  def listAllFiles(dir: String) = {
    
  }
}

object ZipBasicParserTester {

  def main(args: Array[String]): Unit = {
    val f = "~/Downloads/hadoop-trunk.zip"
    import com.betterdocs.crawler.ZipBasicParser._
    val files: (ArrayBuffer[(String, String)], List[String]) = readJavaFiles(new ZipFile(f))
    files._1.map(x => generateTokensWRTImports(extractImports(x._2, files._2), x._2)).foreach(x => println(x.mkString("\n")))
  }
}
