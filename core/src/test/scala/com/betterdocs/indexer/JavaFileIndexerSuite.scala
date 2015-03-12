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

import java.io.StringWriter

import com.betterdocs.crawler.Repository
import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class JavaFileIndexerSuite extends FunSuite with BeforeAndAfterAll {
  val stream =
    Thread.currentThread().getContextClassLoader.getResourceAsStream("TransportClient.java")
  val writer = new StringWriter()
  val allOccurrences = List(67, 68, 70, 73, 74, 75, 101, 105, 108, 109, 111, 123,
    137, 141, 144, 145, 147, 158, 172, 187, 188, 189, 191, 198, 203)
  val sampleRepo = Repository("sample", 0, "sample", false, "Java", "master", 0)
  override def beforeAll() {
    IOUtils.copy(stream, writer)
  }

  test("Parse a file and verify tokens when lines of context is more than file size") {
    val javaFileIndexer = new JavaFileIndexer {
      override val linesOfContext = 2000
    }
    val result = javaFileIndexer.generateTokens(Map("sample-master/Sample.java" -> writer.toString),
      List(), Some(Repository.empty))
    assert(result.size === 1)
    assert(result.head.lineNumbers === allOccurrences)
  }

  test("Parse a file and verify tokens when lines of context is much less than file size") {
    val javaFileIndexer = new JavaFileIndexer {
      override val linesOfContext = 10
    }
    val result = javaFileIndexer.generateTokens(
      Map("sample-master/Sample.java" -> writer.toString), List(), Some(Repository.empty))
    val occurrences = result.map(x => x.lineNumbers).reduce(_ ++ _).sorted.distinct
    assert(occurrences === allOccurrences)
    assert(result.size == 40)
  }

  test("Excluded imports should not be part of Tokens") {
    val javaFileIndexer = new JavaFileIndexer {
      override val linesOfContext = 20
    }
    val excludes = Set("org.apache.spark", "org.apache", "org",
      "org.apache.spark.network.protocol")
    val resultWOExcludes = javaFileIndexer.generateTokens(Map("sample-master/Sample.java" ->
      writer.toString), List(), Some(Repository.empty))
    val resultWExcludes = javaFileIndexer.generateTokens(Map("sample-master/Sample.java" ->
      writer.toString), excludes.toList, Some(Repository.empty))
    val expected = resultWOExcludes.flatMap(x => x.strings)
      .filterNot(_.startsWith("org.apache.spark.network.protocol"))
    val result = resultWExcludes.flatMap(x => x.strings)
    assert(expected === result)
  }

}
