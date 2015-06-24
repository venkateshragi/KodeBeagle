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

import java.util
import java.util.{ArrayList, HashMap}

import scala.collection.immutable.Map
import com.kodebeagle.indexer.{MethodAndLines, MethodToken, Token}

object MethodVisitorHelper {

  def getImportsWithMethodAndLineNumbers(parser: MethodVisitor, tokens: List[Map[String,
    List[Int]]]): List[(Map[String, Map[String, List[Int]]], Map[String, List[Int]])] = {
      import scala.collection.JavaConversions._
      val zippedMethodsWithTokens = parser.getImportsWithMethodAndLineNumber.toList zip tokens
      zippedMethodsWithTokens.map { methodsAndTokens => val (methods, tokens) = methodsAndTokens
          (tokens map { token =>
              val (importName, _) = token
              if (methods.containsKey(importName)) {
                      importName -> javaToScalaMap(methods)(importName)
                  }
              else importName -> Map[String, List[Int]]()
          }, tokens)
      }
  }

  def getImports(parser: MethodVisitor, excludePackages: Set[String]): Set[(String, String)] = {
    import scala.collection.JavaConversions._
    parser.getImportDeclMap.toIterator.map(x => (x._2.stripSuffix(s".${x._1}"),
      x._1)).filterNot { case (left, right) => excludePackages.contains(left) }.toSet
  }

  def getTokenMap(parser: MethodVisitor, importsSet: Set[String]): List[Map[String, List[Int]]] = {
    import scala.collection.JavaConversions._
    val listOflineNumbersMap: util.ArrayList[util.HashMap[String, util.ArrayList[Integer]]] =
      parser.getListOflineNumbersMap
    listOflineNumbersMap.map { x =>
        x.map(y => Token(y._1.toLowerCase, y._1, y._2.map(_.toInt).toSet))
      .filter{ x => importsSet.contains(x.importExactName)}.toSet
    }.toList.map { a =>
      a.map { a => a.importExactName -> a.lineNumbers.toList }.toMap
    }
  }

  def javaToScalaMap(
      javaHashMap:
      HashMap[String, HashMap[String, ArrayList[Integer]]]):
  Map[String, Map[String, List[Int]]] = {
    import scala.collection.JavaConversions._
    (javaHashMap map { case (k, v) =>
      k -> v.toMap.map {
        case (k, v) => k -> v.toList.distinct.map(_.toInt)
      }
    }).toMap
  }

  def createMethodIndexEntries(parser: MethodVisitor,
                               tokens: List[Map[String, List[Int]]]): List[Set[MethodToken]] =
      getImportsWithMethodAndLineNumbers(parser, tokens).map { methodTokens =>
        val (method, tokens) = methodTokens
      createMethodIndexEntry(method, tokens)
    }

  def createMethodIndexEntry(importWithMethods: Map[String, Map[String, List[Int]]],
                             tokens: Map[String, List[Int]]): Set[MethodToken] = {
    importWithMethods.map { case (importName, methodAndLineNumbers) =>
      MethodToken(importName.toLowerCase, importName, tokens(importName),
        methodAndLineNumbers.map {
          case (k, v) => MethodAndLines(k, v)
        }.toSet
      )
    }
  }.toSet

}
