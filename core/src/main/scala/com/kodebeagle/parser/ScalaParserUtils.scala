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

import com.kodebeagle.indexer.{MethodAndLines, HighLighter}
import org.scalastyle.{LineColumn, CheckerUtils, Lines}

import scala.collection.mutable.ListBuffer
import scala.util.Try
import scalariform.lexer.{Token, Tokens}
import scalariform.parser.{AstNode, CallExpr, FullDefOrDcl, FunDefOrDcl, Param, ParamClause, TemplateBody, TmplDef}

object ScalaParserUtils {

  def getImportsLines(funDefOrDcl: FunDefOrDcl, param: String, lines: Lines): List[HighLighter] = {
    funDefOrDcl.tokens.filter(_.rawText == param).map(getHighLighter(_, lines))
  }

  private def getHighLighter(token: Token, lines: Lines) = {
    val lineColumn = lines.toLineColumn(token.offset) match {
      case Some(x) => x
      case None => LineColumn(-1, -1)
    }
    HighLighter(lineColumn.line, lineColumn.column, lineColumn.column + token.range.length)
  }

  def extractFunctions(source: String): List[FunDefOrDcl] = {
    val buffer = ListBuffer[FunDefOrDcl]()
    try {
      new CheckerUtils().parseScalariform(source).foreach(
        x => x.ast.topStats.immediateChildren.foreach {
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
    } catch {
      case e: Exception =>
    }
    buffer.toList
  }

  def getCallExprAndLines(callExprs: List[CallExpr], param: String, lines: Lines):
  Iterable[MethodAndLines] = {
    val matchedCallExprs = callExprs.filter(callExpr =>
      callExpr.tokens.map(_.rawText).contains(param))
    val methodsAndOffset = matchedCallExprs.flatMap(callExpr =>
      getMethodAndOffset(callExpr.tokens.toArray, param))
    val methodsVsLineColumn = methodsAndOffset.groupBy(_._1).mapValues(_.map(_._2))
      .mapValues(_.map(lines.toLineColumn).map(_.get))
    methodsVsLineColumn.map(x => MethodAndLines(x._1, x._2.map(lineCol =>
      HighLighter(lineCol.line, lineCol.column, lineCol.column + x._1.length))))
      .filter(_.lineNumbers.nonEmpty)
  }

  def getAllCallExprs(funDefOrDcl: FunDefOrDcl): List[CallExpr] = {
    val buff = ListBuffer[CallExpr]()
    def recursiveVisit(children: List[AstNode]): Unit = {
      children.map {
        case callExpr: CallExpr => buff += callExpr
        case other => if (other.immediateChildren.nonEmpty) {
          recursiveVisit(other.immediateChildren)
        } else {
          recursiveVisit(List[AstNode]())
        }
      }
    }
    funDefOrDcl.funBodyOpt match {
      case Some(x) => recursiveVisit(x.immediateChildren)
      case None =>
    }
    buff.toList
  }

  private def getMethodAndOffset(tokens: Array[Token], param: String): Array[(String, Int)] = {
    val optionTupleOfTokens = tokens.zipWithIndex.flatMap(x => x._1.rawText == "." match {
      case true => Some(Try(tokens(x._2 - 1)).toOption, Try(tokens(x._2 + 1)).toOption)
      case false => None
    })
    val identityToken = Token(Tokens.VARID, "", -1, "")
    val tupleOfTokens = optionTupleOfTokens.map(x => (x._1.getOrElse(identityToken),
      x._2.getOrElse(identityToken)))
    tupleOfTokens.filter(_._1.rawText == param).map(x => (x._2.rawText, x._2.offset))
  }

  def getParams(funDefOrDcl: FunDefOrDcl): List[(String, String)] = {
    def getAllParams(paramClause: ParamClause) = {
      val mutableList = ListBuffer[Param]()
      paramClause.firstParamOption match {
        case Some(param) => mutableList += param
        case None =>
      }
      mutableList ++= paramClause.otherParams.map(_._2)
      mutableList.toList
    }

    funDefOrDcl.paramClauses.paramClausesAndNewlines.flatMap(x => {
      val allParams = getAllParams(x._1)
      allParams.map(x => (x.id.rawText, x.paramTypeOpt.get._2.tokens
        .filter(_.tokenType == Tokens.VARID).map(_.rawText).headOption match {
        case Some(text) => text
        case None => ""
      })).filter(_._2.nonEmpty)
    })
  }
}
