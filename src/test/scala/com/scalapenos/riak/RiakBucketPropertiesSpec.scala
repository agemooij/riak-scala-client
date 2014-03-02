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

class RiakBucketPropertiesSpec extends RiakClientSpecification with RandomKeySupport {
  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)

  "A RiakBucket" should {
    "support setting and getting the bucket properties" in {
      val bucket = randomBucket
      val oldProperties = bucket.properties.await

      val newNumberOfReplicas = oldProperties.numberOfReplicas + 1
      val newAllowSiblings = !oldProperties.allowSiblings
      val newLastWriteWins = !oldProperties.lastWriteWins

      (bucket.properties = Set(NumberOfReplicas(newNumberOfReplicas),
                               AllowSiblings(newAllowSiblings),
                               LastWriteWins(newLastWriteWins))).await

      val newProperties = bucket.properties.await

      newProperties.numberOfReplicas must beEqualTo(newNumberOfReplicas)
      newProperties.allowSiblings must beEqualTo(newAllowSiblings)
      newProperties.lastWriteWins must beEqualTo(newLastWriteWins)

      bucket.numberOfReplicas.await must beEqualTo(newNumberOfReplicas)
      bucket.n_val.await must beEqualTo(newNumberOfReplicas)

      bucket.allowSiblings.await must beEqualTo(newAllowSiblings)
      bucket.allow_mult.await must beEqualTo(newAllowSiblings)

      bucket.lastWriteWins.await must beEqualTo(newLastWriteWins)
      bucket.last_write_wins.await must beEqualTo(newLastWriteWins)
    }

    "support setting the bucket properties with an empty set (nothing happens)" in {
      val bucket = randomBucket
      val oldProperties = bucket.properties.await

      bucket.setProperties(Set()).await

      val newProperties = bucket.properties.await

      oldProperties must be equalTo(newProperties)
    }

    "support directly setting the 'n_val' bucket property" in {
      val bucket = randomBucket

      (bucket.numberOfReplicas = 5).await
      bucket.numberOfReplicas.await must beEqualTo(5)

      (bucket.numberOfReplicas = 3).await
      bucket.numberOfReplicas.await must beEqualTo(3)
    }

    "support directly setting the 'allow_mult' bucket property" in {
      val bucket = randomBucket

      (bucket.allowSiblings = true).await
      bucket.allowSiblings.await must beTrue

      (bucket.allowSiblings = false).await
      bucket.allowSiblings.await must beFalse
    }

    "support directly setting the 'last_write_wins' bucket property" in {
      val bucket = randomBucket

      (bucket.lastWriteWins = true).await
      bucket.lastWriteWins.await must beTrue

      (bucket.lastWriteWins = false).await
      bucket.lastWriteWins.await must beFalse
    }

    "fail when directly setting the 'n_val' bucket property to any integer smaller than 1" in {
      val bucket = randomBucket

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
