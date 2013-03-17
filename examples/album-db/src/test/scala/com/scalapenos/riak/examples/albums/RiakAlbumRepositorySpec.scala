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

package com.scalapenos.riak.examples.albums

import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._
import akka.actor._
import akka.testkit._
import spray.util._

import com.scalapenos.riak._

import AlbumRepositoryProtocol._


class RiakAlbumRepositorySpec extends Specification with NoTimeConversions {
  val timeout = 2 seconds

  val album1 = Album(
    title = "Employment",
    artist = "Kaiser Chiefs",
    releasedIn = 2005,
    tracks = List(
      Track(1,  "Everyday I Love You Less and Less"),
      Track(2,  "I Predict a Riot"),
      Track(3,  "Modern Way"),
      Track(4,  "Na Na Na Na Naa"),
      Track(5,  "You Can Have It All"),
      Track(6,  "Oh My God"),
      Track(7,  "Born to Be a Dancer"),
      Track(8,  "Saturday Night"),
      Track(9,  "What Did I Ever Give You?"),
      Track(10, "Time Honoured Tradition"),
      Track(11, "Caroline, Yes"),
      Track(12, "Team Mate")
    )
  )


  "When receiving a FetchAlbumByTitle message, the RiakAlbumRepository" should {
    "reply with a Some[Album] if the album exists" in new AkkaTestkitContext {
      val albumRepository = TestActorRef(Props[RiakAlbumRepository])

      storeAlbumInDatabase(album1)

      within(timeout) {
        albumRepository ! FetchAlbumByTitle(album1.title)

        val albumFromDb = expectMsgType[Option[RiakMeta[Album]]]

        albumFromDb must beSome[RiakMeta[Album]]
        albumFromDb.get.data must beEqualTo(album1)
      }

      removeAlbumFromDatabase(album1)
    }

    "reply with a None if the album doesn't exist" in new AkkaTestkitContext {
      val albumRepository = TestActorRef(Props[RiakAlbumRepository])
      val title = "...Baby One More Time"

      verifyAlbumDoesNotExistInDatabase(title)

      within(timeout) {
        albumRepository ! FetchAlbumByTitle(title)

        val albumFromDb = expectMsgType[Option[RiakMeta[Album]]]

        albumFromDb must beNone
      }
    }
  }

  "when receiving a StoreAlbum message, the RiakAlbumRepository" should {
    "store the album in the database" in new AkkaTestkitContext {
      val albumRepository = TestActorRef(Props[RiakAlbumRepository])

      verifyAlbumDoesNotExistInDatabase(album1.title)

      within(timeout) {
        albumRepository ! StoreAlbum(album1)

        val albumFromDb = expectMsgType[RiakMeta[Album]]

        albumFromDb.data must beEqualTo(album1)

        verifyAlbumExistsInDatabase(album1)
      }

      removeAlbumFromDatabase(album1)
    }
  }

  "when receiving an UpdateAlbum message, the RiakAlbumRepository" should {
    "store the album in the database" in new AkkaTestkitContext {
      val albumRepository = TestActorRef(Props[RiakAlbumRepository])
      val updatedAlbum = album1.copy(tracks = Track(13, "I Heard It Through the Grapevine") +: album1.tracks)

      storeAlbumInDatabase(album1)

      within(timeout) {
        albumRepository ! FetchAlbumByTitle(album1.title)

        val albumFromDb = expectMsgType[Option[RiakMeta[Album]]]

        albumFromDb must beSome[RiakMeta[Album]]
        albumFromDb.get.data must beEqualTo(album1)

        albumRepository ! UpdateAlbum(albumFromDb.get.map(_ => updatedAlbum))

        val updatedAlbumFromDb = expectMsgType[RiakMeta[Album]]

        updatedAlbumFromDb.data must beEqualTo(updatedAlbum)

        verifyAlbumExistsInDatabase(updatedAlbum)
      }

      removeAlbumFromDatabase(updatedAlbum)
    }
  }


  // ==========================================================================
  // Test Helpers
  // ==========================================================================

  abstract class AkkaTestkitContext extends TestKit(ActorSystem()) with ImplicitSender with After {
    def after {
      system.shutdown()
    }
  }

  private def storeAlbumInDatabase(album: Album)(implicit system: ActorSystem) {
    // we use the .await implit view from spray.)util to block on the outcome here.
    // This is fine in unit tests to make them predictable but should never be used in production
    // so this is NOT a best practise for how to use Futures!
    bucket.store(album.title, album).await

    verifyAlbumExistsInDatabase(album)
  }

  private def removeAlbumFromDatabase(album: Album)(implicit system: ActorSystem) {
    bucket.delete(album.title).await

    verifyAlbumDoesNotExistInDatabase(album.title)
  }

  private def verifyAlbumExistsInDatabase(album: Album)(implicit system: ActorSystem) {
    val dbValue = bucket.fetch(album.title).await

    dbValue must beSome[RiakValue]

    val albumInDb = dbValue.get.as[Album]

    albumInDb must beEqualTo(album)
  }

  private def verifyAlbumDoesNotExistInDatabase(title: String)(implicit system: ActorSystem) {
    bucket.fetch(title).await must beNone
  }

  private def bucket(implicit system: ActorSystem) = RiakClient(system).bucket("albums")
}


