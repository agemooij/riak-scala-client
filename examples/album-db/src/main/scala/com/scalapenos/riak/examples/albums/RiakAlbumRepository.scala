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

import akka.actor._
import com.scalapenos.riak._


object AlbumRepositoryProtocol {
  case class StoreAlbum(album: Album)
  case class UpdateAlbum(album: RiakMeta[Album])
  case class FetchAlbumByTitle(title: String)
}

class RiakAlbumRepository extends Actor with ActorLogging {
  import AlbumRepositoryProtocol._
  import context.dispatcher

  private val albums = RiakClient(context.system, "localhost", 8098).bucket("albums")

  def receive = {
    case StoreAlbum(album)        => storeAlbum(album, sender)
    case UpdateAlbum(album)       => updateAlbum(album, sender)
    case FetchAlbumByTitle(title) => fetchAlbumByTitle(title, sender)
  }

  private def storeAlbum(album: Album, actor: ActorRef) {
    albums.storeAndFetch(album.title, album)
          .map(value => value.asMeta[Album])
          .onSuccess {
            case storedAlbumMeta => actor ! storedAlbumMeta
          }
  }

  private def updateAlbum(albumMeta: RiakMeta[Album], actor: ActorRef) {
    albums.storeAndFetch(albumMeta.data.title, albumMeta)
          .map(value => value.asMeta[Album])
          .onSuccess {
            case storedAlbumMeta => actor ! storedAlbumMeta
          }
  }

  private def fetchAlbumByTitle(title: String, actor: ActorRef) {
    albums.fetch(title)
          .map(valueOption => valueOption.map(_.asMeta[Album]))
          .onSuccess {
            case albumMetaOption => actor ! albumMetaOption
          }
  }
}
