/*
 * Copyright (C) 2012-2013 Age Mooij (http://scalapenos.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scalapenos.riak

import org.slf4j.{Logger, LoggerFactory}
import spray.json.DefaultJsonProtocol._
import spray.json.JsString
import scala.concurrent.Future

class RiakBucketSearchSpec extends RiakClientSpecification with RandomKeySupport {

  case class SongTestComplex (number: Int, title: String, data:Map[String, String])
  object SongTestComplex {
    implicit val jsonFormat = jsonFormat3(SongTestComplex.apply)
  }

  val logger = LoggerFactory.getLogger("com.scalapenos.riak")

  val randomIndex = randomKey

  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)


  "A RiakClient" should {
    "create a search index" in {
      client.createSearchIndex(randomIndex).await must beTrue
    }
    "get a search by name" in {
      val test = client.getSearchIndex(randomIndex).await
      true must beTrue
    }
    "get a list of all search index" in {
      val test = client.getSearchIndexList.await
      true must beTrue
    }
    "delete a search index" in {
      client.deleteSearchIndex(randomIndex).await must beTrue
    }
  }

  "A RiakBucket" should {
    "get an empty precommit and set precommit values for solr search" in {
      pending
      val bucket = randomBucket
      val key = randomKey

      val properties = bucket.getProperties.await

      properties.preCommit must beEqualTo(List.empty[Map[String, Any]])

      val precommitValues = List(
        Map(
          "mod" -> "riak_search_kv_hook",
          "fun" -> "precommit"
        )
      )

      (bucket.preCommit = precommitValues).await

      val propertiesNew = bucket.getProperties.await

      val searchHook = precommitValues(0)

      propertiesNew.preCommit.contains(
        searchHook.mapValues(JsString(_))) must beTrue
    }

    "insert two elements and search with solr to get them back" in {
      pending
      val bucket = randomBucket

      val songComplex1 = SongTestComplex(1, "titulo1", Map("test1" -> "datatest1"))
      bucket.store(s"$randomKey-song1", songComplex1).await

      val songComplex2 = SongTestComplex(2, "titulo2", Map("test2" -> "datatest2"))
      bucket.store(s"$randomKey-song2", songComplex2).await

      val solrQuery = RiakSearchQuery()
      solrQuery.wt(Some(JSONSearchFormat()))
      solrQuery.q(Some("title:titulo*"))

      val query = bucket.search(solrQuery).await
      val listValues =
        query.responseValues.values.map(_.map(_.get.as[SongTestComplex]).await)

      listValues.contains(songComplex1) must beTrue
      listValues.contains(songComplex2) must beTrue

    }
  }

}