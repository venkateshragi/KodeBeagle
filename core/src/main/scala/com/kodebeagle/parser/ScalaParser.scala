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

import org.scalastyle.CheckerUtils

import scala.collection.mutable.ListBuffer
import scala.util.Try
import scalariform.parser.{FullDefOrDcl, FunDefOrDcl, TemplateBody, TmplDef}
import scalariform.utils.Range

case class TypeInFunction(typeName: String, ranges: List[Range], props: Set[(String, List[Range])])

private class ScalaParser(funDefOrDcl: FunDefOrDcl, imports: Set[(String, String)])
  extends ScalaParserBase(funDefOrDcl) {

  def parseFunction(): List[TypeInFunction] = {
    val allCallExprs = getAllCallExprs
    val listOfTupleOfParamType = getListOfParamVsType.filter(x => imports.map(_._2).contains(x._2))
    listOfTupleOfParamType.map { paramType =>
      val usageLines = getUsageRanges(paramType._1)
      val mapOfPropertyAndLine = getCallExprAndRanges(allCallExprs, paramType._1).toSet
      val fqImportName = getImportName(imports, paramType._2)
      TypeInFunction(fqImportName, usageLines, mapOfPropertyAndLine)
    }
  }


  def getImportName(imports: Set[(String, String)], className: String): String = {
    def tuple2ToImportString(importName: (String, String)): String = {
      importName._1 + "." + importName._2
    }

    imports.find(_._2 == className) match {
      case Some(x) => tuple2ToImportString(x)
      case None => ""
    }
  }
}

object ScalaParser {

  def parse(source: String, imports: Set[(String, String)]): List[List[TypeInFunction]] = {
    extractFunctions(source).map { funDef =>
      val scalaParser = new ScalaParser(funDef, imports)
      scalaParser.parseFunction()
    }
  }

  def extractFunctions(source: String): List[FunDefOrDcl] = {
    val buffer = ListBuffer[FunDefOrDcl]()
    val mayBeScalariformAst = Try(new CheckerUtils().parseScalariform(source))
    if (mayBeScalariformAst.isSuccess) {
      mayBeScalariformAst.get.foreach(x =>
        x.ast.topStats.immediateChildren.foreach {
          case fullDefOrDcl: FullDefOrDcl => fullDefOrDcl.defOrDcl match {
            case tmplDef: TmplDef => tmplDef.templateBodyOption match {
              case Some(tmplBody: TemplateBody) => tmplBody.statSeq.otherStats.foreach(_._2.foreach
              (_.immediateChildren.foreach {
                case funDefOrDecl: FunDefOrDcl => buffer += funDefOrDecl
                case _ =>
              }))
              case _ =>
            }
            case _ =>
          }
          case _ =>
        }
      )
    }
    buffer.toList
  }
}
