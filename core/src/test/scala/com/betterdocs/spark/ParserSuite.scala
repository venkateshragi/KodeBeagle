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

package com.betterdocs.spark

import com.betterdocs.crawler.Repository
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class ParserSuite extends FunSuite with BeforeAndAfterAll {

  test("Simple repo") {
    val r = RepoFileNameParser("repo~apache~zookeeper~160999~false~Java~trunk~789.zip")
    assert(r == Some(Repository("apache", 160999, "zookeeper", false, "Java", "trunk", 789)))
  }

  test("Names with special character") {
    val r = RepoFileNameParser("repo~apache~zookeeper-lost~160999~false~Java~=+-trunk/2.1~789.zip")
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

