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
import scala.concurrent.duration._

class RiakBucketSearchSpec extends RiakClientSpecification with RandomKeySupport {

  case class SongTestComplex (number_i: Int, title_s: String, data_b:Map[String, String])
  object SongTestComplex {
    implicit val jsonFormat = jsonFormat3(SongTestComplex.apply)
  }

  val logger = LoggerFactory.getLogger("com.scalapenos.riak")

  val randomIndex = randomKey

  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)

  val bucket = randomBucket

  sequential

  "A RiakClient" should {
    "create a search index" in {
      client.createSearchIndex(randomIndex, 3).await must beAnInstanceOf[RiakSearchIndex]
    }
    "get a search by name" in {
      Thread.sleep(10000)
      client.getSearchIndex(randomIndex).await must beAnInstanceOf[RiakSearchIndex]
    }
    "get a list of all search index" in {
      client.getSearchIndexList.await must beAnInstanceOf[List[RiakSearchIndex]]
    }
    "assign a search index, insert two elements and search with solr to get them back" in {

      val riakIndex = client.getSearchIndex(randomIndex).await
      val indexAssigned = (bucket.setSearchIndex(riakIndex)).await

      //Wait for index ready before searching
      Thread.sleep(10000)

      val songComplex1 = SongTestComplex(1, "titulo1", Map("test1" -> "datatest1"))
      bucket.store(s"$randomKey-song1", songComplex1).await

      val songComplex2 = SongTestComplex(2, "titulo2", Map("test2" -> "datatest2"))
      bucket.store(s"$randomKey-song2", songComplex2).await

      Thread.sleep(10000)

      val solrQuery = RiakSearchQuery()
      solrQuery.wt(Some(JSONSearchFormat()))
      solrQuery.q(Some("title_s:titulo*"))

      val query = bucket.search(solrQuery).await
      val listValues =
        query.responseValues.values.map(_.map(_.get.as[SongTestComplex]).await)

      listValues.contains(songComplex1) must beTrue
      listValues.contains(songComplex2) must beTrue

    }
    "throw an error when delete a search index assigned to a bucket" in {
      client.deleteSearchIndex(randomIndex).await must throwA[Exception]
    }
    "delete a search index" in {
      (bucket.setSearchIndex(RiakSearchIndex("_dont_index_", 0, "_dont_index_"))).await
      Thread.sleep(10000)
      client.deleteSearchIndex(randomIndex).await must beTrue
    }
  }

}