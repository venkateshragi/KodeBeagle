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

package com.kodebeagle.parser

import java.util.{ArrayList, HashMap}

import scala.collection.immutable.Map
import com.kodebeagle.indexer._

object MethodVisitorHelper {

  def getImportsWithMethodAndLineNumbers(parser: MethodVisitor, tokens: List[Map[String,
    List[ExternalLine]]]): List[(Map[String, Map[String, List[ExternalLine]]],
    Map[String, List[ExternalLine]])] = {
    import scala.collection.JavaConversions._
    val zippedMethodsWithTokens = parser.importsMethodsAndLineColumnNumbersList.toList zip tokens
    zippedMethodsWithTokens.map { methodsAndTokens => val (methods, tokens) = methodsAndTokens
      (tokens map { token =>
        val (importName, _) = token
        if (methods.containsKey(importName)) {
          importName -> javaToScalaMap(methods)(importName)
        }
        else importName -> Map[String, List[ExternalLine]]()
      }, tokens)
    }
  }

  def getTokenMap(parser: MethodVisitor, importsSet: Set[String]):
  List[Map[String, List[ExternalLine]]] = {
    import scala.collection.JavaConversions._

    parser.lineAndColumnsNumbers.map { x =>
      x.map(y =>
        Token(y._1.toLowerCase, y._1, y._2.map(a =>
          ExternalLine(a._1.toInt, a._2.toInt, a._3.toInt)).toSet)).filter
        { x => importsSet.contains(x.importExactName) }.toSet
    }.toList.map { a =>
      a.map { a => a.importExactName -> a.lineNumbers.toList }.toMap
    }
  }

  def javaToScalaMap(
    javaHashMap:
    HashMap[String, HashMap[String, ArrayList[(Integer,Integer,Integer)]]]):
  Map[String, Map[String, List[ExternalLine]]] = {
    import scala.collection.JavaConversions._
    (javaHashMap map { case (k, v) =>
      k -> v.toMap.map {
        case (k, v) => k -> v.toList.map(a => ExternalLine(a._1, a._2, a._3))
      }
    }).toMap
  }

  def createMethodIndexEntries(parser: MethodVisitor,
    tokens: List[Map[String, List[ExternalLine]]]): List[Set[ExternalType]] =
    getImportsWithMethodAndLineNumbers(parser, tokens).map { methodTokens =>
      val (method, tokens) = methodTokens
      createMethodIndexEntry(method, tokens)
    }

  def createMethodIndexEntry(importWithMethods: Map[String, Map[String, List[ExternalLine]]],
                             tokens: Map[String, List[ExternalLine]]): Set[ExternalType] = {
    importWithMethods.map { case (importName, methodAndLineNumbers) =>
      ExternalType(importName, tokens(importName),
        methodAndLineNumbers.map {
          case (k, v) => Property(k, v)
        }.toSet
      )
    }
  }.toSet

}
