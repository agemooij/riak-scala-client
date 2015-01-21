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

import akka.event.slf4j.Logger
import spray.json.DefaultJsonProtocol._

class RiakBucketTypeSpec extends RiakClientSpecification with RandomKeySupport {

  Logger("riak-scala-client").warn("a bucket type with name bucket-type-test must be manually created")

  val bucketType = client.bucketType("bucket-type-test")
  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey, bucketType = bucketType)

  val randomBucketForSearch = client.bucket("riak-bucket-tests-" + randomKey, bucketType = bucketType)

  case class SongTestComplex (number_i: Int, title_s: String, data_b:Map[String, String])
  object SongTestComplex {
    implicit val jsonFormat = jsonFormat3(SongTestComplex.apply)
  }

  var searchIndex:RiakSearchIndex = _
  trait searchIndexSpec extends org.specs2.mutable.Before {
    def before = {
      try {
        searchIndex = client.getSearchIndex("test-search-index").await
      } catch {
        case x:OperationFailed =>
          searchIndex = client.createSearchIndex("test-search-index").await
      }
    }
  }

  "A RiakBucketType" should {
    "throw an exception when try to search in an bucket and the bucket type do not have an index associated" in new searchIndexSpec {
      val bucket = randomBucket

      val solrQuery = RiakSearchQuery()
      solrQuery.wt(Some(JSONSearchFormat()))
      solrQuery.q(Some("title_s:titulo*"))

      bucket.bucketType.search(solrQuery).await must throwAn[OperationFailed]
    }

    "be able to be assigned with a search index" in new searchIndexSpec{
      val bucket = randomBucketForSearch
      val indexAssigned = (bucket.bucketType.setSearchIndex(searchIndex)).await
      indexAssigned must beTrue.eventually
    }

    "be able to search based on it's index" in new searchIndexSpec {
      val bucket = randomBucketForSearch

      Thread.sleep(10000)

      val songComplex1 = SongTestComplex(1, "titulo1", Map("test1" -> "datatest1"))
      bucket.store(s"song1", songComplex1).await

      val songComplex2 = SongTestComplex(2, "titulo2", Map("test2" -> "datatest2"))
      bucket.store(s"song2", songComplex2).await

      val solrQuery = RiakSearchQuery()
      solrQuery.wt(Some(JSONSearchFormat()))
      solrQuery.q(Some("title_s:titulo*"))

      val query = bucket.bucketType.search(solrQuery).await

      val listValues = query.responseValues.values.map( values => values.map(_.as[SongTestComplex])).await

      listValues.contains(songComplex1) must beTrue

    }

    "be able to remove it's index" in {
      bucketType.setSearchIndex(RiakNoSearchIndex).await must beTrue.eventually
    }
  }

}
