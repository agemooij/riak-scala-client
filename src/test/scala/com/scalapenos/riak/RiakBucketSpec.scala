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
  val bucket = client.bucket("riak-bucket-tests-" + randomKey)

  "A RiakBucket" should {
    "support storing any class T if there is a Serializer[T] and a RiakIndexer[T]in scope" in {
      pending
    }

    "support getting the bucket properties" in {
      val properties = bucket.properties.await

      properties.numberOfReplicas must beEqualTo(3)
      properties.allowSiblings must beFalse
      properties.lastWriteWins must beFalse
    }

    "support setting the bucket properties" in {
      pending
    }

    "support setting the bucket properties with an empty set (nothing happens)" in {
      pending
    }

    "support setting and getting the 'n_val' bucket property" in {
      pending
    }

    "support setting and getting the 'allow_mult' bucket property" in {
      pending
    }

    "support setting and getting the 'last_value_wins' bucket property" in {
      pending
    }
  }

}
