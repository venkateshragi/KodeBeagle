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

package com.kodebeagle.spark

import com.kodebeagle.configuration.KodeBeagleConfig
import com.kodebeagle.indexer.{JavaASTBasedIndexerForMethods, Repository, ScalaASTBasedIndexer, Statistics}
import com.kodebeagle.javaparser.JavaASTParser
import com.kodebeagle.spark.SparkIndexJobHelper._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object CreateIndexJobForMethods {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster(KodeBeagleConfig.sparkMaster)
      .setAppName("CreateIndexJobForMethods")
        .set("spark.driver.memory", "6g")
        .set("spark.executor.memory", "4g")
      .set("spark.network.timeout", "1200s")

    val sc: SparkContext = createSparkContext(conf)

    val zipFileExtractedRDD: RDD[(List[(String, String)], List[(String, String)],
      Option[Repository], List[String], Statistics)] = makeZipFileExtractedRDD(sc)

    val javaASTBasedIndexerForMethods = new JavaASTBasedIndexerForMethods()
    val parser: JavaASTParser = new JavaASTParser(true)
    val pars = sc.broadcast(parser)
    // Create indexes for elastic search.

    zipFileExtractedRDD.map { f =>
      val (javaFiles, scalaFiles, repo, packages, stats) = f
      (repo, javaASTBasedIndexerForMethods
        .generateTokensWithMethods(javaFiles.toMap, packages, repo),
        ScalaASTBasedIndexer.generateTokensWithFunctionContext(scalaFiles.toMap, packages, repo),
        mapToSourceFiles(repo, javaFiles ++ scalaFiles), stats,
        CreateFileMetaData.getFilesMetaData(mapToSourceFiles(repo, javaFiles), pars))
    }.flatMap { case (Some(repository), javaIndices, scalaIndices, sourceFiles,
    stats, filesMetaData) =>
      Seq(toJson(repository, isToken = false), toJson(javaIndices, isToken = false),
        toLangSpecificJson("importsmethods", "typescala", scalaIndices, isToken = false),
        toJson(sourceFiles, isToken = false), toJson(stats, isToken = false),
        toJson(filesMetaData, isToken = false))
    case _ => Seq()
    }.saveAsTextFile(KodeBeagleConfig.sparkIndexOutput)
  }
}
