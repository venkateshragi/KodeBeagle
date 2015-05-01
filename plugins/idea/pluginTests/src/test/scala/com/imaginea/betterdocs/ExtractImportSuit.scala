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

package com.imaginea.betterdocs

import java.io.InputStream

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.collection.JavaConversions._

class ExtractImportSuit extends FunSuite with BeforeAndAfterAll {
  private val stream: InputStream =
    Thread.currentThread.getContextClassLoader.getResourceAsStream("TestData.java")

  private val fileContents = IOUtils.readLines(stream).mkString("\n")

  private val document: Document = new DocumentImpl(fileContents)

  test("Extracted imports should match the imports in java file.") {
    val editorDocOps = new EditorDocOps().getImports(document)
    val expected = Set(
      "java.io.Closeable",
      "java.io.IOException",
      "java.util.UUID",
      "java.util.concurrent.ExecutionException",
      "java.util.concurrent.TimeUnit",
      "com.google.common.base.Objects",
      "com.google.common.base.Preconditions",
      "com.google.common.base.Throwables",
      "com.google.common.util.concurrent.SettableFuture",
      "io.netty.channel.Channel",
      "io.netty.channel.ChannelFuture",
      "io.netty.channel.ChannelFutureListener",
      "org.slf4j.Logger",
      "org.slf4j.LoggerFactory",
      "org.apache.spark.network.protocol.ChunkFetchRequest",
      "org.apache.spark.network.protocol.RpcRequest",
      "org.apache.spark.network.protocol.StreamChunkId",
      "org.apache.spark.network.util.NettyUtils"
    )
    assert(editorDocOps.toSet === expected)
  }

}