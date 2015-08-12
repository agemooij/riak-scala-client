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

class RiakBucketSpec extends RiakClientSpecification with RandomKeySupport with RandomBucketSupport {

  "A RiakBucket" should {
    "not be able to store an empty String value" in {
      val bucket = randomBucket
      val key = randomKey

      // Riak will reject the request with a 400 because the request will
      // not have a body (because Spray doesn't allow empty bodies).
      bucket.store(key, "").await must throwA[BucketOperationFailed]
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

    "fetch all sibling values and return them to the client if they exist for a given Riak entry" in {
      val bucket = randomBucket
      val key = randomKey

      (bucket.allowSiblings = true).await

      val siblingValues = Set("value1", "value2", "value3")

      for (value ← siblingValues) {
        // we store values without VectorClock which causes siblings creation
        bucket.store(key, value).await
      }

      val fetched = bucket.fetchWithSiblings(key).await

      fetched should beSome
      fetched.get.size should beEqualTo(3)
      fetched.get.map(_.data) should beEqualTo(siblingValues)
    }

    "return a set containing a single value for given Riak entry if there are no siblings when fetching with siblings mode" in {
      val bucket = randomBucket
      val key = randomKey

      (bucket.allowSiblings = true).await

      val expectedValue = "value"
      bucket.store(key, expectedValue).await

      val fetched = bucket.fetchWithSiblings(key).await

      fetched should beSome
      fetched.get.size should beEqualTo(1)
      fetched.get.map(_.data) should beEqualTo(Set(expectedValue))
    }

    "return None if entry hasn't been found when fetching with siblings mode" in {
      val bucket = randomBucket
      val key = randomKey

      (bucket.allowSiblings = true).await

      val fetched = bucket.fetchWithSiblings(key).await

      fetched should beNone
    }
  }
}
