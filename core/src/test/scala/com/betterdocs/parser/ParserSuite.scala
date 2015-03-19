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

package com.betterdocs.parser

import java.io.InputStream

import com.betterdocs.crawler.Repository
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class ParserSuite extends FunSuite with BeforeAndAfterAll {

  test("Simple repo") {
    val r = RepoFileNameParser("repo~apache~zookeeper~160999~false~Java~trunk~789.zip")
    assert(r == Some(Repository("apache", 160999, "zookeeper", false, "Java", "trunk", 789)))
  }

  test("Names with special character") {
    val r = RepoFileNameParser("/home/dir~temp/repo~apache~zookeeper-lost~160999~false~Java" +
      "~=+-trunk/2.1~789.zip")
    assert(r == Some(Repository("apache", 160999, "zookeeper-lost", false, "Java", "=+-trunk/2.1",
      789)))
  }

  test("Branch name with version number only") {
    val r = RepoFileNameParser("repo~apache~zookeeper~160999~false~Java~2.1~789.zip")
    assert(r == Some(Repository("apache", 160999, "zookeeper", false, "Java", "2.1", 789)))
  }


  test("Branch name with tilde in it.") {
    val r = RepoFileNameParser("repo~apache~zookee~per~160999~false~Java~2.~1~789.zip")
    assert(r == None)
  }

  test("Branch name absent.") {
    val r = RepoFileNameParser("repo~apache~zookeeper~160999~false~Java~789.zip")
    assert(r == Some(Repository("apache", 160999, "zookeeper", false, "Java", "master", 789)))
  }
}

class MethodVisitorSuite extends FunSuite with BeforeAndAfterAll {

  import scala.collection.JavaConversions._

  test("Verify method visitor includes lines with usages.") {
    val stream: InputStream =
      Thread.currentThread.getContextClassLoader.getResourceAsStream("TransportClient.java")

    val m: MethodVisitor = new MethodVisitor
    m.parse(stream)
    val callstack: Map[String, List[String]] = m.getMethodCallStack.map(x => (x._1, x._2
      .toList)).toMap
    val lines = m.getLineNumbersMap.toMap.values.flatMap(x => x.map(_.toInt)).toList.sorted.distinct
    assert(lines === List(79, 101, 102, 103, 106, 108, 137, 138, 139, 141, 142, 144, 172, 187,
      189, 191, 198, 203))
  }
}