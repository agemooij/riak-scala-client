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

      bucket.storeAndFetch(key, "value").await

      bucket.fetch(key, IfModifiedSince(DateTime.now)).await must beNone
    }

    "return back a stored value if 'If-Modified-Since' condition holds for requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      bucket.fetch(key, IfModifiedSince(DateTime.now.minusMinutes(5))).await must beSome(storedValue)
    }

    "not return back a stored value if 'If-Unmodified-Since' condition does not hold for a requested data" in {
      val bucket = randomBucket
      val key = randomKey

      bucket.storeAndFetch(key, "value").await

      bucket.fetch(key, IfUnmodifiedSince(DateTime.now.minusMinutes(5))).await must beNone
    }

    "return back a stored value if 'If-Unmodified-Since' condition holds for requested data" in {
      val bucket = randomBucket
      val key = randomKey

      val storedValue = bucket.storeAndFetch(key, "value").await

      bucket.fetch(key, IfUnmodifiedSince(DateTime.now.plusMinutes(5))).await must beSome(storedValue)
    }
  }
}
