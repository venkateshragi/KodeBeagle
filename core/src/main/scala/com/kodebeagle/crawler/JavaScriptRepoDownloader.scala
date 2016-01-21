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

package com.kodebeagle.crawler

import com.kodebeagle.configuration.KodeBeagleConfig
import com.kodebeagle.crawler.GitHubApiHelper._
import com.kodebeagle.crawler.GitHubRepoDownloader.DownloadJavaScriptRepos
import com.kodebeagle.logging.Logger
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.Try

object JavaScriptRepoDownloader extends App with Logger {
  implicit val format = DefaultFormats

  private val client = new HttpClient()

  val perPageRepositories = args(0).toInt

  var pageNumber = args(1).toInt
  var token: String = KodeBeagleConfig.githubTokens(0)

  def skippedCount(pageNumber: Int): Int = perPageRepositories * pageNumber

  GitHubRepoDownloader.repoDownloader ! DownloadJavaScriptRepos(pageNumber)

  def startCrawlingFromSkippedCount(pageNumber: Int): Unit = {
    try {
      val method = new GetMethod(url(perPageRepositories, skippedCount(pageNumber)))
      val executedMethod = executeMethod(method)
      val ids = httpGetJson(executedMethod).get \ "rows"
      if (ids.children.isEmpty) log.info("Did not get repo list from NPM")
      else {
        val repoIdVsGithubUrls = getRepoAndGithubUrlMap(ids)
        log.info("[" + repoIdVsGithubUrls + "]")
        repoIdVsGithubUrls.foreach { case (repoId, repoURL) =>
          if (repoURL.nonEmpty && repoURL.size == 1) {
            val repo = repoURL(0)
            log.info("#repo : " + repo)
            val gitUserNameAndRepoName = repo.split("/")
            val userName = gitUserNameAndRepoName(0).split(":")(1)
            val lastIndex = gitUserNameAndRepoName(1).lastIndexOf(".git")
            val repoName = if (lastIndex == -1) gitUserNameAndRepoName(1)
            else gitUserNameAndRepoName(1).substring(0, gitUserNameAndRepoName(1).
              lastIndexOf(".git"))
            log.info(s"Downloading from pageNumber $pageNumber " +
              s"https://api.github.com/repos/$userName/${repoName}")
            val method = GitHubApiHelper.executeMethod(s"https://api.github.com/" +
              s"repos/$userName/${repoName}", token)
            httpGetJson(method).map { json =>
              val repository = extractRepoInfo(json)
              GitHubApiHelper.cloneRepository(repository, repoURL.head, KodeBeagleConfig.githubDir)
            }
          }
        }
      }
    } catch {
      case ex: Exception => log.error("Exception {}", ex)
    }
  }

  def getRepoAndGithubUrlMap(ids: JValue): Map[String, List[String]] = {
    val repoIdVsGithubUrls = ids.children.map { repoDetails =>
      val repoId = (repoDetails \ "id").values.toString
      val method = new GetMethod(getRepoUrl(repoId))
      val executedMethod = executeMethod(method)
      repoId -> (httpGetJson(executedMethod).get \ "versions").children.map(item =>
        ((item \ "repository" \ "url").values.toString.replaceAll(
          "git\\+https://github.com/|git\\+ssh://git@github.com/|https://github.com/|" +
            "git://github.com/|http://github.com/", "git@github.com:"))).
        filter(repo => repo.contains("github.com")).distinct
    }
    repoIdVsGithubUrls.toMap
  }

  def url(limit: Int, skip: Int): String = s"https://skimdb.npmjs.com/registry/_all_docs?" +
    s"limit= $limit&skip=$skip"

  def getRepoUrl(repoName: String): String = s"https://skimdb.npmjs.com/registry/$repoName"

  def httpGetJson(method: GetMethod): Option[JValue] = {
    val status = method.getStatusCode
    if (status == 200) {
      // ignored parsing errors if any, because we can not do anything about them anyway.
      Try(parse(method.getResponseBodyAsStream)).toOption
    } else {
      log.error("Request failed with status:" + status + "Response:"
        + method.getResponseHeaders.mkString("\n") +
        "\nResponseBody " + method.getResponseBodyAsString)
      None
    }
  }

  def executeMethod(method: GetMethod): GetMethod = {
    client.executeMethod(method)
    method
  }
}

