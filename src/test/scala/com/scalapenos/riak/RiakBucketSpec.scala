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

class RiakBucketSpec extends RiakClientSpecification with RandomKeySupport {
  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)
  private var riakSearchIndex:Option[RiakSearchIndex] = None

  sequential

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

      (bucket.allowMult = true).await

      bucket.store(key, "value").await
      bucket.delete(key).await

      val fetched = bucket.fetch(key).await

      fetched should beNone
    }

    "be able to be created with bucket type" in {
      val bucketType = client.bucketType("bucketTypeTest")
      val bucket = client.bucket(name="riak-bucket-tests-" + randomKey, bucketType=bucketType)

      bucket must beAnInstanceOf[RiakBucket]
    }

    "get bucket type from a bucket created with a custom bucket type" in {
      val bucketType = client.bucketType("bucketTypeTest")
      val bucket = client.bucket(name="riak-bucket-tests-" + randomKey, bucketType=bucketType)

      bucket.bucketType.name must be_==(bucketType.name)
    }

    "get default bucket type from a bucket created with default bucket type" in {
      val bucket = client.bucket(name="riak-bucket-tests-" + randomKey)

      bucket.bucketType.name must be_==("default")
    }

    "get a list of bucket keys" in {
      val bucket = client.bucket(name="riak-bucket-tests-" + randomKey)
      val key = randomKey
      bucket.store(key, "value").await

      bucket.getKeys().await.contains(key) must beTrue
    }

    "get a list of bucket keys using stream" in {
      val bucket = client.bucket(name="riak-bucket-tests-" + randomKey)
      bucket.store("mustHaveKey", "mustHaveValue").await

      var lString = List.empty[String]

      val keysAsStream = bucket.getKeysStream()

      keysAsStream.onChunk{
        x => lString = lString ++ x.chunk
      }
      keysAsStream.onFinish{
        x => lString = lString ++ x.chunk
      }

      lString.contains("mustHaveKey") must beTrue.eventually
    }
  }

}
