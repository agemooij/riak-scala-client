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

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import java.util.UUID._


class RiakBucketSpec extends RiakClientSpecification with RandomKeySupport {
  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)

  "A RiakBucket" should {
    "be able to store and fetch an empty String value" in {
      val bucket = randomBucket
      val key = randomKey

      bucket.store(key, "").await

      val fetched = bucket.fetch(key).await

      fetched should beSome[RiakValue]
      fetched.get.data must beEqualTo("")
    }

    "treat tombstone values as if they don't exist when allow_mult = false" in {
      val bucket = randomBucket
      val key = randomKey

      bucket.store(key, "value").await
      bucket.delete(key).await

      val fetched = bucket.fetch(key).await

      fetched should beNone
    }

    "treat tombstone values as if they don't exist when allow_mult = true" in {
      val bucket = randomBucket
      val key = randomKey

      (bucket.allowSiblings = true).await

      bucket.store(key, "value").await
      bucket.delete(key).await

      val fetched = bucket.fetch(key).await

      fetched should beNone
    }
  }

}
