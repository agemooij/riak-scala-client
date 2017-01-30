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

import java.util.UUID.randomUUID
import java.util.zip.ZipException

import scala.concurrent.Future

class RiakGzipSpec extends AkkaActorSystemSpecification {

  // ============================================================================
  // Test data
  // ============================================================================

  val key = "foo"
  val expectedValue = "bar"

  // ============================================================================
  // Specifications
  // ============================================================================

  "Riak client with an optional compression support" should {
    "be able to have 2 clients (with & without compression) working with the same bucket where allowed_siblings=true" in {

      val compressionDisabledClient = createRiakClient(false)
      val compressionEnabledClient = createRiakClient(true)

      val bucketName = s"$baseBucketName-$randomUUID"

      val bucket = compressionEnabledClient.bucket(bucketName, fixedConflictResolver)

      // Enable siblings for this bucket
      bucket.setAllowSiblings(true).await

      // Check there is no initial data
      val fetchBeforeStore = bucket.fetch(key)
      fetchBeforeStore.await must beNone

      // This creates 2 siblings in the Riak bucket
      bucket.storeAndFetch(key, "initial_bar").await
      // but the expected value should win as we use a "fixed" conflict resolver
      bucket.storeAndFetch(key, expectedValue).await

      // Try to fetch it with both clients (with and without compression)
      checkFetch(compressionEnabledClient, bucketName, key, expectedValue, Some(fixedConflictResolver))
      checkFetch(compressionDisabledClient, bucketName, key, expectedValue, Some(fixedConflictResolver))
    }

    "be able to have 2 clients (with & without compression) working with the same bucket where allowed_siblings=false" in {

      val gzipDisabledClient = createRiakClient(false)
      val gzipEnabledClient = createRiakClient(true)

      val bucketName = s"$baseBucketName-$randomUUID"

      val bucket = gzipEnabledClient.bucket(bucketName)

      // Disable siblings for this bucket
      bucket.setAllowSiblings(false).await

      // Check there is no initial data
      val fetchBeforeStore = bucket.fetch(key)
      fetchBeforeStore.await must beNone

      // Put the expected value in Riak
      bucket.storeAndFetch(key, expectedValue).await

      // Try to fetch it with both clients (with and without compression)
      checkFetch(gzipEnabledClient, bucketName, key, expectedValue)
      checkFetch(gzipDisabledClient, bucketName, key, expectedValue)
    }

    "be able to have 2 clients (with compression) trying to delete same values from the same bucket" in {

      /*
       * This test scenario covers an interesting Riak behaviour when returning 'Not Found 404' responses to 'delete key' requests.
       *
       * Sometimes, Riak can return a 404 response with a text/plain body - 'not found'. However, this response MIGHT have a "Content-Encoding: gzip" header.
       * Trying to decompress the payload of such response leads to an internal "java.util.zip.ZipException: Not in GZIP format" exception in the riak client's http pipeline.
       */

      val BatchSize = 1000
      val BucketName = s"$baseBucketName-$randomUUID"

      val client1 = createRiakClient(true)
      val client2 = createRiakClient(true)

      val bucket1 = client1.bucket(BucketName)
      val bucket2 = client2.bucket(BucketName)

      def createKey(i: Int) = s"foo-$i"

      // First store initial values
      val storeFutures =
        for {
          i ← 1 to BatchSize
        } yield bucket1.storeAndFetch(createKey(i), "bar")

      // Wait till all store operations succeed.
      Future.sequence(storeFutures).await

      // Now try to delete those keys twice in parallel.
      val deleteFutures1 =
        for {
          i ← 1 to BatchSize
        } yield bucket1.delete(createKey(i))

      val deleteFutures2 =
        for {
          i ← 1 to BatchSize
        } yield bucket2.delete(createKey(i))

      // Wait for both deletion batches to succeed.
      Future.sequence(deleteFutures1).await must not(throwAn[ZipException])
      Future.sequence(deleteFutures2).await must not(throwAn[ZipException])
    }
  }

  // ============================================================================
  // Helpers
  // ============================================================================

  private def baseBucketName = "test-client-compression-support"

  private val fixedConflictResolver = new RiakConflictsResolver {
    // Resolves conflicts by always preferring the expected value
    override def resolve(values: Set[RiakValue]): ConflictResolution =
      ConflictResolution(
        values.find(_.data == expectedValue).getOrElse(throw new Exception("No expected value in siblings")),
        writeBack = false)
  }

  private def checkFetch(client: RiakClient,
    bucketName: String,
    key: String,
    expectedValue: String,
    conflictResolver: Option[RiakConflictsResolver] = None) = {
    val bucket =
      conflictResolver.map(resolver ⇒ client.bucket(bucketName, resolver)).getOrElse(client.bucket(bucketName))

    val fetchAfterStore = bucket.fetch(key).await

    fetchAfterStore must beSome[RiakValue]
    fetchAfterStore.get.data must beEqualTo(expectedValue)
  }
}
