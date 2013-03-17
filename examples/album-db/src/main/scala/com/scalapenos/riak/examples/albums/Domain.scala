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

import com.scalapenos.riak._
import com.scalapenos.riak.MediaTypes._

import spray.json.DefaultJsonProtocol._


case class Track (number: Int, title: String)
object Track {
  implicit val jsonFormat = jsonFormat2(Track.apply)
}

case class Album (
  title: String,
  artist: String,
  releasedIn: Int,
  tracks: List[Track]
)

object Album {
  // Uncomment this line and comment out the custom serialization object below to revert back to the default spray-json serialization
  // implicit val jsonFormat = jsonFormat4(Album.apply)

  implicit object customXmlSerialization extends RiakSerializer[Album] with RiakDeserializer[Album] {
    def serialize(album: Album): (String, ContentType) = {
      val xml = <album>
                  <title>{album.title}</title>
                  <artist>{album.artist}</artist>
                  <releasedin>{album.releasedIn}</releasedin>
                  <tracks>
                  {album.tracks.map(track => <track><number>{track.number}</number><title>{track.title}</title></track>)}
                  </tracks>
                </album>.toString

       (xml, ContentType(`text/xml`))
    }

    /* Of course this would normally need a lot of error handling... */
    def deserialize(data: String, contentType: ContentType): Album = {
      import scala.xml._
      def stringToXmlElem = XML.load(new java.io.StringReader(data))
      def xmlElemToTrack(elem: Elem) = Track((elem \ "number").text.toInt, (elem \ "title").text)
      def xmlElemToAlbum(elem: Elem) = Album(
        (elem \ "title").text,
        (elem \ "artist").text,
        (elem \ "releasedin").text.toInt,
        (elem \\ "track").toList.map(node => xmlElemToTrack(node.asInstanceOf[Elem]))
      )

      contentType match {
        case ContentType(`text/xml`, _) => xmlElemToAlbum(stringToXmlElem)
        case _ => throw RiakUnsupportedContentType(ContentType(`text/xml`), contentType)
      }
    }
  }

  implicit object indexer extends RiakIndexer[Album] {
    def index(album: Album) = Set(RiakIndex("artist", album.artist),
                                  RiakIndex("releasedIn", album.releasedIn))
  }
}
