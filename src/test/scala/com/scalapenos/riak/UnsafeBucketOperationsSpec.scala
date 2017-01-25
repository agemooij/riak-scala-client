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

class UnsafeBucketOperationsSpec extends RiakClientSpecification with RandomKeySupport with RandomBucketSupport {

  def randomUnsafeBucketOperations = randomBucket(client).unsafe

  "UnsafeBucketOperations" should {
    "list all keys" in {
      val unsafeBucketOperations = randomUnsafeBucketOperations
      val numberOfKeys = 5
      val keys = (1 to numberOfKeys).map(_ ⇒ randomKey)

      keys.foreach { key ⇒
        unsafeBucketOperations.store(key, "value").await
      }

      val allKeys = unsafeBucketOperations.allKeys().await

      keys.foreach { key ⇒
        unsafeBucketOperations.delete(key).await
      }

      allKeys.keys.toSet must beEqualTo(keys.toSet)
    }

    "list all keys from an empty unsafeBucketOperations" in {
      val unsafeBucketOperations = randomUnsafeBucketOperations

      val allKeys = unsafeBucketOperations.allKeys().await

      allKeys.keys should be(Nil)
    }
  }
}
