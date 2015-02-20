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

package com.betterdocs.crawler

import java.io.File
import java.net.URL

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.io.FileUtils
import org.apache.spark.Logging
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.Try

case class Repository(login: String, id: Int, name: String, fork: Boolean, language: String,
                      stargazersCount: Int)

/**
 * This class relies on Github's {https://developer.github.com/v3/} Api.
 */
object GitHubApiHelper extends Logging {

  implicit val format = DefaultFormats
  private val client = new HttpClient()

  /**
   * Access Github's
   * [[https://developer.github.com/v3/repos/#list-all-public-repositories List all repositories]]
   * @param since Specify id of repo to start the listing from. (Pagination)
   */
  def getAllGitHubRepos(since: Int): List[Map[String, String]] = {
    val json = httpGetJson(s"https://api.github.com/repositories?since=$since").toList
    // Here we can specify all the fields we need from repo query.
    val interestingFields = List("full_name", "fork")
    for {j <- json
         c <- j.children
         map = (for {
           JObject(child) <- c
           JField(name, value) <- child
           if interestingFields.contains(name)
         } yield name -> value.values.toString).toMap
    } yield map
  }

  /**
   * Parallel fetch is not worth trying since github limits per user limit of 5000 Req/hr.
   */
  def fetchDetails(repoMap: Map[String, String]): Option[Repository] = {
    for {
      repo <- httpGetJson("https://api.github.com/repos/" + repoMap("full_name"))
    } yield Repository((repo \ "owner" \ "login").extract[String],
      (repo \ "id").extract[Int], (repo \ "name").extract[String], (repo \ "fork").extract[Boolean],
      (repo \ "language").extract[String], (repo \ "stargazers_count").extract[Int])
  }

  /*
   * Helper for accessing Java - Apache Http client. (It it important to stick with the current version and all.)
   */
  def httpGetJson(url: String) = {
    val method = new GetMethod(url)
    method.setDoAuthentication(true)
    // Please add the oauth token instead of <token> here. Or github may give 403/401 as response.
    method.addRequestHeader("Authorization", "token <token>")
    val status = client.executeMethod(method)
    if (status == 200) {
      Try(parse(method.getResponseBodyAsString)).toOption // ignored parsing errors if any, because we can not do anything about them anyway.
    } else {
      logError("Request failed with status:" + status + "Response:" + method.getResponseHeaders.mkString("\n") +
        "\nResponseBody " + method.getResponseBodyAsString)
      None
    }
  }

  def downloadRepository(r: Repository, targetDir: String): File = {
    try {
      val repoFile = new File(s"$targetDir/repo~${r.login}~${r.name}~${r.id}~${r.fork}~${r.language}~${r.stargazersCount}.zip")
      logInfo(s"Downloading $repoFile")
      // FIXME: master is not always present - or is default branch but then there is an extra service call to find out.
      FileUtils.copyURLToFile(new URL(s"https://github.com/${r.login}/${r.name}/archive/master.zip"), repoFile)
      repoFile
    } catch {
      case x: Throwable => logError(s"Failed to download $r", x)
        null
    }
  }
}

object GitHubApiHelperTest {
  def main(args: Array[String]): Unit = {
    import com.betterdocs.crawler.GitHubApiHelper._
    for (i <- Range(23000, 300000, 350)) yield getAllGitHubRepos(i).filter(x => x("fork") == "false").distinct
        .map(fetchDetails).flatten.distinct.filter(x => x.language == "Java" && !x.fork)
        .map(x => downloadRepository(x, "~/github"))
  }
}
