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

import com.kodebeagle.logging.Logger

import scala.util.Try
import scala.util.parsing.combinator._

/** A generic parser that understands any language indented with curly brackets. */
object GenericBraceMatchingParser extends JavaTokenParsers with Logger {

  def recursiveFlatten(l: Any): List[String] = {
    l match {
      case List(x: List[Any]) => recursiveFlatten(x)
      case x1 :: xtail => recursiveFlatten(x1) ++ recursiveFlatten(xtail)
      case y: String => List[String](y)
      case List(x: String) => List[String](x)
      case Nil => List[String]()
    }
  }

  def apply(s: String): Option[List[String]] = Try(parseAll(all, s)).toOption.flatMap {
    case Success(result, _) => Some(recursiveFlatten(result))
    case failure: NoSuccess => log.error("Got:" + failure.msg +
      " while parsing at:" + failure.next.offset)
      None
  }

  def block: Parser[List[Any]] = """{""" ~> codeBlock <~ """}"""

  def code: Parser[String] = """[^\{\}]+""".r

  def codeBlock: Parser[List[Any]] = rep(code ~ block ^^ { case (x ~ y) => List(x, y) } | code)

  /** This will keep the innermost blocks and ignore anything in the common section */
  def codeBlockKeepInnerMostOnly: Parser[List[Any]] = rep(code ~> block | code)

  def all: Parser[List[Any]] = codeBlock
}
