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

  "A RiakBucket" should {
    "support storing any class T if there is a Serializer[T] and a RiakIndexer[T]in scope" in {
      pending
    }

    "support getting the bucket properties" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)
      val properties = bucket.properties.await

      properties.numberOfReplicas must beEqualTo(3)
      properties.allowSiblings must beFalse
      properties.lastWriteWins must beFalse
    }

    "support getting the 'n_val' bucket property directly" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)

      bucket.numberOfReplicas.await must beEqualTo(3)
      bucket.n_val.await must beEqualTo(3)
    }

    "support getting the 'allow_mult' bucket property directly" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)

      bucket.allowSiblings.await must beFalse
      bucket.allow_mult.await must beFalse
    }

    "support getting the 'last_write_wins' bucket property directly" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)

      bucket.lastWriteWins.await must beFalse
      bucket.last_write_wins.await must beFalse
    }

    "support setting the bucket properties" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)
      val oldProperties = bucket.properties.await

      (bucket.properties = Set(NumberOfReplicas(5), AllowSiblings(true), LastWriteWins(true))).await

      val newProperties = bucket.properties.await

      oldProperties.numberOfReplicas must not be equalTo(newProperties.numberOfReplicas)
      oldProperties.allowSiblings must not be equalTo(newProperties.allowSiblings)
      oldProperties.lastWriteWins must not be equalTo(newProperties.lastWriteWins)
    }

    "support setting the bucket properties with an empty set (nothing happens)" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)
      val oldProperties = bucket.properties.await

      bucket.setProperties(Set()).await

      val newProperties = bucket.properties.await

      oldProperties must be equalTo(newProperties)
    }

    "support directly setting the 'n_val' bucket property" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)

      bucket.numberOfReplicas.await must beEqualTo(3)

      (bucket.numberOfReplicas = 5).await

      bucket.numberOfReplicas.await must beEqualTo(5)
    }

    "support directly setting the 'allow_mult' bucket property" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)

      bucket.allowSiblings.await must beFalse

      (bucket.allowSiblings = true).await

      bucket.allowSiblings.await must beTrue
    }

    "support directly setting the 'last_write_wins' bucket property" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)

      bucket.lastWriteWins.await must beFalse

      (bucket.lastWriteWins = true).await

      bucket.lastWriteWins.await must beTrue
    }

    "fail when directly setting the 'n_val' bucket property to any integer smaller than 1" in {
      val bucket = client.bucket("riak-bucket-tests-" + randomKey)

      (bucket.numberOfReplicas = 0).await must throwA[IllegalArgumentException]
      (bucket.numberOfReplicas = -1).await must throwA[IllegalArgumentException]
      (bucket.numberOfReplicas = -1000).await must throwA[IllegalArgumentException]
    }

    "fail when creating an instance of NumberOfReplicas (n_val) with any integer smaller than 1" in {
      NumberOfReplicas(0) must throwA[IllegalArgumentException]
      NumberOfReplicas(-1) must throwA[IllegalArgumentException]
      NumberOfReplicas(-42) must throwA[IllegalArgumentException]
    }
  }

}
