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

import org.apache.commons.compress.archivers.zip.ZipFile

import scala.collection.JavaConversions._
import scala.collection.mutable
import java.util.regex.Pattern

import scala.util.Try

/**
 * Parses java files from the given zip file.
 */
object ZipBasicParser {

  private val importPattern: Pattern = Pattern.compile("import (.*)\\.(\\w+);")
  private val linesOfContext: Int = 3

  private val bufferSize = 102400

  def readJavaFiles(zip: ZipFile) = {
    val list = mutable.ArrayBuffer[(String, String)]()
    val allJavaFiles =
      zip.getEntries.buffered.filter(x => x.getName.endsWith("java") && !x.isDirectory).toTraversable
    val files = allJavaFiles.map(x => x.getName -> new BufferedInputStream(zip.getInputStream(x)))
    for ((name, f) <- files) {
      val b = new Array[Byte](bufferSize)
      f.read(b)
      list += (name -> new String(b).trim)
    }
    list
  }

  def extractImports(java: String) = java.split("\n").map(x => importPattern.matcher(x)).filter(_.find)
    .map(x => Try(x.group(1) -> x.group(2).trim).toOption).flatten

  def generateTokensWRTImports(imports: (String, String), java: String) = {
    val lines = java.split("\n")
    lines.sliding(linesOfContext).map { x =>
      x.mkString("\n").
    }
  }

}

object ZipBasicParserTester {

  def main(args: Array[String]): Unit = {
    val f = "/home/prashant/github/repo~ymirpl~nsaper~18117~false~Java~2.zip"
    ZipBasicParser.readJavaFiles(new ZipFile(f)).map(x => ZipBasicParser.extractImports(x._2))
  }
}
