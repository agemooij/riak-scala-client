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

import spray.json.JsString

class RiakBucketPropertiesSpec extends RiakClientSpecification with RandomKeySupport {
  private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)

  "A RiakBucket" should {
    "support setting and getting the bucket properties" in {
      val bucket = randomBucket
      val oldProperties = bucket.getProperties.await
      println(oldProperties)

      val newNumberOfReplicas = oldProperties.nVal + 1
      val newAllowSiblings = !oldProperties.allowMult
      val newLastWriteWins = !oldProperties.lastWriteWins

      bucket.setProperties(Set(NumberOfReplicas(newNumberOfReplicas),
                               AllowMult(newAllowSiblings),
                               LastWriteWins(newLastWriteWins))).await

      val newProperties = bucket.getProperties.await

      newProperties.nVal must beEqualTo(newNumberOfReplicas)
      newProperties.allowMult must beEqualTo(newAllowSiblings)
      newProperties.lastWriteWins must beEqualTo(newLastWriteWins)

      bucket.nVal.await must beEqualTo(newNumberOfReplicas)
      bucket.nVal.await must beEqualTo(newNumberOfReplicas)

      bucket.allowMult.await must beEqualTo(newAllowSiblings)
      bucket.allowMult.await must beEqualTo(newAllowSiblings)

      bucket.lastWriteWins.await must beEqualTo(newLastWriteWins)
      bucket.lastWriteWins.await must beEqualTo(newLastWriteWins)
    }

    "support setting the bucket properties with an empty set (nothing happens)" in {
      val bucket = randomBucket
      val oldProperties = bucket.getProperties.await

      bucket.setProperties(Set()).await

      val newProperties = bucket.getProperties.await

      oldProperties must be equalTo(newProperties)
    }

    "support directly setting the 'nVal' bucket property" in {
      val bucket = randomBucket

      (bucket.nVal = 5).await
      bucket.nVal.await must beEqualTo(5)

      (bucket.nVal = 3).await
      bucket.nVal.await must beEqualTo(3)
    }

    "support directly setting the 'allowMult' bucket property" in {
      val bucket = randomBucket

      (bucket.allowMult = true).await
      bucket.allowMult.await must beTrue

      (bucket.allowMult = false).await
      bucket.allowMult.await must beFalse
    }

    "support directly setting the 'lastWriteWins' bucket property" in {
      val bucket = randomBucket

      (bucket.lastWriteWins = true).await
      bucket.lastWriteWins.await must beTrue

      (bucket.lastWriteWins = false).await
      bucket.lastWriteWins.await must beFalse
    }

    "fail when directly setting the 'nVal' bucket property to any integer smaller than 1" in {
      val bucket = randomBucket

      (bucket.nVal = 0).await must throwA[IllegalArgumentException]
      (bucket.nVal = -1).await must throwA[IllegalArgumentException]
      (bucket.nVal = -1000).await must throwA[IllegalArgumentException]
    }

    "fail when creating an instance of NumberOfReplicas (nVal) with any integer smaller than 1" in {
      NumberOfReplicas(0) must throwA[IllegalArgumentException]
      NumberOfReplicas(-1) must throwA[IllegalArgumentException]
      NumberOfReplicas(-42) must throwA[IllegalArgumentException]
    }

    "get an empty precommit and set precommit values for old solr search as example" in {
      val bucket = randomBucket
      val key = randomKey

      val properties = bucket.getProperties.await

      properties.preCommit must beEqualTo(List.empty[Map[String, Any]])

      val precommitValues = List(
        Map(
          "mod" -> "riak_search_kv_hook",
          "fun" -> "precommit"
        )
      )

      (bucket.preCommit = precommitValues).await

      val propertiesNew = bucket.getProperties.await

      propertiesNew.preCommit must beEqualTo(precommitValues)
    }

  }

}
