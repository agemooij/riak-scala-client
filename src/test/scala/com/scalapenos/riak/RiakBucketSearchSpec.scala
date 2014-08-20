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

  case class SongTestComplex (number: Int, title: String, data:Map[String, String])
  object SongTestComplex {
    implicit val jsonFormat = jsonFormat3(SongTestComplex.apply)
  }

  val logger = LoggerFactory.getLogger("com.scalapenos.riak")

  val randomIndex = randomKey

  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)


  "A RiakClient" should {
    "create a search index" in {
      client.createSearchIndex(randomIndex, 4).await must beAnInstanceOf[RiakSearchIndex]
      Thread.sleep(5000)
    }
    "get a search by name" in {
      client.getSearchIndex(randomIndex).await must beAnInstanceOf[RiakSearchIndex]
    }
    "get a list of all search index" in {
      client.getSearchIndexList.await must beAnInstanceOf[List[RiakSearchIndex]]
    }
    "delete a search index" in {
      client.deleteSearchIndex(randomIndex).await must beTrue
    }
  }

}