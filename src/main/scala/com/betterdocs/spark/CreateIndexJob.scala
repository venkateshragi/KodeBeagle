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

import com.betterdocs.crawler.ZipBasicParser
import com.betterdocs.indexer.{JavaFileIndexer, Token}
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

object CreateIndexJob {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[6]").setAppName("CreateIndexJob")
    val sc = new SparkContext(conf)
    // TODO: It is possible to write following very optimally.
    sc.binaryFiles("/home/prashant/github").map(x =>
      // Ignoring exclude packages.
      (ZipBasicParser.readFilesAndPackages(new ZipFile(x._1.stripPrefix("file:")))._1,
        getGitScore(x._1))).flatMap { f => val (files, score) = f
      new JavaFileIndexer().generateTokens(files.toMap, List(), score.getOrElse(0))
    }.map(toJson).saveAsTextFile("tokens") // This job take about 10 min+ to finish on 1Gb of zips.

  }

  /**
   * This currently uses star counts for a repo as a score.
   */
  def getGitScore(f: String): Option[Int] = {
    val score = Try(f.stripSuffix(".zip").split("~").last.toInt).toOption
    score
  }

  def toJson(t: Token): String = {
    import org.json4s._
    import org.json4s.jackson.Serialization
    import org.json4s.jackson.Serialization.write
    implicit val formats = Serialization.formats(NoTypeHints)
    write(t)
  }

}

object CreateIndex {

  def main(args: Array[String]): Unit = {
    import com.betterdocs.crawler.ZipBasicParser._
    val indexer: JavaFileIndexer = new JavaFileIndexer
    import indexer._
    for (f <- listAllFiles("/home/prashant/github/")) {
      val zipFile = Try(new ZipFile(f))
      zipFile match {
        case Success(zf) =>
          val files: (ArrayBuffer[(String, String)], List[String]) = readFilesAndPackages(zf)
          val score: Option[Int] = CreateIndexJob.getGitScore(f.getName)
          generateTokens(files._1.toMap, files._2, score.getOrElse(0)).map(CreateIndexJob.toJson)
            .foreach(println)
        case Failure(e) => println(s"$f failed because ${e.getMessage}")
      }
    }
  }
}
