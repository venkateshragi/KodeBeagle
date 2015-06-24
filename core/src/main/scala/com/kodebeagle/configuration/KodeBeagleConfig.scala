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

package com.kodebeagle.configuration

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

object KodeBeagleConfig {
  val config = ConfigFactory.load()
  var lastIndex=0

  private val settings = scala.collection.mutable.HashMap[String, String]()

  for (c <- config.entrySet())
    yield settings.put(c.getKey, c.getValue.unwrapped.toString)
  // Output dir for spark job.
  private[kodebeagle] val sparkIndexOutput = get("kodebeagle.spark.index.outputDir").get
  private[kodebeagle] val sparkRepoOutput = get("kodebeagle.spark.repo.outputDir").get
  private[kodebeagle] val sparkSourceOutput = get("kodebeagle.spark.source.outputDir").get
  private[kodebeagle] val sparkMethodsOutput = get("kodebeagle.spark.method.outputDir").get

  private[kodebeagle] val linesOfContext = get("kodebeagle.indexing.linesOfContext").get
  // This is required to use GithubAPIHelper
  private[kodebeagle] val githubTokens: Array[String] =
    get("kodebeagle.spark.githubTokens").get.split(",")
  // This is required to use GithubAPIHelper
  private[kodebeagle] val githubDir: String = get("kodebeagle.github.crawlDir").get
  private[kodebeagle] val sparkMaster: String = get("kodebeagle.spark.master").get

  def nextToken(arr: Array[String] = githubTokens): String = {
    if (lastIndex == arr.length - 1) {
      lastIndex = 0
      arr(lastIndex)
    } else {
      lastIndex = lastIndex + 1
      arr(lastIndex)
    }
  }

  def get(key: String): Option[String] = settings.get(key)

  def set(key: String, value: String) {
    if (Option(key) == None || Option(value) == None) {
      throw new NullPointerException("key or value can't be null")
    }
    settings.put(key, value)
  }

}
