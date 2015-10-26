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

import org.scalastyle.{CheckerUtils, ScalariformAst}
import scala.util.Try
import scalariform.lexer.Tokens

object ScalaParser {

  // It is WIP, Offsets have to be captured from starting def to ending '}'
  // I just felt it is easier to write a parser myself. Here goes `GenericBraceMatchingParser`.
  def parse(s: String): Unit = {
    val scalariform: Option[ScalariformAst] = new CheckerUtils().parseScalariform(s)
    scalariform.map { x=>
      x.ast.tokens.filter(_.tokenType == Tokens.DEF).map(_.text)
    }

  }
}
