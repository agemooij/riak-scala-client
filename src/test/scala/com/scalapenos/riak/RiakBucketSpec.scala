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

import com.scalapenos.riak.RiakBucket.{ IfMatch, IfModifiedSince, IfNotMatch, IfUnmodifiedSince }
import org.joda.time.DateTime

class RiakBucketSpec extends RiakClientSpecification with RandomKeySupport with RandomBucketSupport {

  "A RiakBucket" should {
    "not be able to store an empty String value" in {
      val bucket = randomBucket(client)
      val key = randomKey

      // Riak will reject the request with a 400 because the request will
      // not have a body (because Spray doesn't allow empty bodies).
      bucket.store(key, "").await must throwA[BucketOperationFailed]
    }

    "treat tombstone values as if they don't exist when allow_mult = false" in {
      val bucket = randomBucket(client)
      val key = randomKey

      bucket.store(key, "value").await
      bucket.delete(key).await

      val fetched = bucket.fetch(key).await

      fetched should beNone
    }

    "treat tombstone values as if they don't exist when allow_mult = true" in {
      val bucket = randomBucket(client)
      val key = randomKey

      (bucket.allowSiblings = true).await

      bucket.store(key, "value").await
      bucket.delete(key).await

      val fetched = bucket.fetch(key).await

      fetched should beNone
    }

    "fetch all sibling values and return them to the client if they exist for a given Riak entry" in {
      val bucket = randomBucket(client)
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
      val bucket = randomBucket(client)
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
      val bucket = randomBucket(client)
      val key = randomKey

      (bucket.allowSiblings = true).await

      val fetched = bucket.fetchWithSiblings(key).await

      fetched should beNone
    }

    // ============================================================================
    // Conditional requests tests
    // ============================================================================

    "not return back a stored value if 'If-None-Match' condition does not hold for a requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      val eTag = storedValue.etag

      bucket.fetch(key, IfNotMatch(eTag)).await must beNone
    }

    "return back a stored value if 'If-None-Match' condition holds for requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      bucket.fetch(key, IfNotMatch(randomKey)).await must beSome(storedValue)
    }

    "not return back a stored value if 'If-Match' condition does not hold for a requested data" in {
      val bucket = randomBucket
      val key = randomKey

      bucket.storeAndFetch(key, "value").await

      bucket.fetch(key, IfMatch(randomKey)).await must beNone
    }

    "return back a stored value if 'If-Match' condition holds for requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      val eTag = storedValue.etag

      bucket.fetch(key, IfMatch(eTag)).await must beSome(storedValue)
    }

    "not return back a stored value if 'If-Modified-Since' condition does not hold for a requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      // Fetch if the value has been modified after store operation
      bucket.fetch(key, IfModifiedSince(storedValue.lastModified.plusMillis(1))).await must beNone
    }

    "return back a stored value if 'If-Modified-Since' condition holds for requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      // Fetch if the value has been modified since before the store operation
      bucket.fetch(key, IfModifiedSince(storedValue.lastModified.minusMinutes(5))).await must beSome(storedValue)
    }

    "not return back a stored value if 'If-Unmodified-Since' condition does not hold for a requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      // Fetch if the value has not been modified since before the store operation
      bucket.fetch(key, IfUnmodifiedSince(storedValue.lastModified.minusMinutes(5))).await must beNone
    }

    "return back a stored value if 'If-Unmodified-Since' condition holds for requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      // Fetch if the value has not been modified since after the store operation
      bucket.fetch(key, IfUnmodifiedSince(storedValue.lastModified.plusMinutes(5))).await must beSome(storedValue)
    }

    // Combining multiple request conditions

    "support multiple conditional request parameters" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      // Fetch a value that hasn't been modified since after the store operation (this condition holds)
      // only if it has a different tag (this condition doesn't hold)
      bucket.fetch(key,
        IfUnmodifiedSince(storedValue.lastModified.plusMillis(1)),
        IfNotMatch(storedValue.etag)
      ).await must beNone

      // Fetch a value if it has the same ETag (this condition holds)
      // has been modified since before the store operation (this condition also holds)
      bucket.fetch(key,
        IfMatch(storedValue.etag),
        IfModifiedSince(storedValue.lastModified.minusMillis(1))
      ).await must beSome(storedValue)

      // Fetch a value if it has the same ETag (this condition holds)
      // and has been modified since after the store operation (this condition doesn't hold)
      bucket.fetch(key,
        IfMatch(storedValue.etag),
        IfModifiedSince(storedValue.lastModified.plusMillis(1))
      ).await must beNone

      bucket.fetch(key,
        // Repeating the same conditional parameter doesn't change the behaviour
        IfNotMatch(storedValue.etag),
        IfNotMatch(storedValue.etag)).await must beNone

      bucket.fetch(key,
        // Repeating the same conditional parameter doesn't change the behaviour
        IfModifiedSince(storedValue.lastModified.minusMinutes(5)),
        IfModifiedSince(storedValue.lastModified.minusMinutes(5))).await must beSome(storedValue)
    }
  }
}
