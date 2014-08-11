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
package internal

import akka.actor._
import spray.http._
import spray.client.pipelining._
import spray.http.parser.HttpParser
import spray.json.{JsString, JsObject}

//Temporary fix for spray 1.3.1_2.11
import java.io.{ ByteArrayOutputStream, ByteArrayInputStream }
import org.jvnet.mimepull.{ MIMEMessage, MIMEConfig }
import org.parboiled.common.FileUtils
import scala.collection.JavaConverters._
import spray.http.parser.HttpParser
import spray.util._
import MediaTypes._
import MediaRanges._
import HttpHeaders._
import HttpCharsets._
import spray.httpx.unmarshalling.Unmarshaller


private[riak] object RiakHttpClientHelper {
  import spray.http.HttpEntity
  import spray.httpx.marshalling._

  /**
   * Spray Marshaller for turning RiakValue instances into HttpEntity instances so they can be sent to Riak.
   */
  implicit val RiakValueMarshaller: Marshaller[RiakValue] = new Marshaller[RiakValue] {
    def apply(riakValue: RiakValue, ctx: MarshallingContext) {
      ctx.marshalTo(HttpEntity(riakValue.contentType, riakValue.data.getBytes(riakValue.contentType.charset.nioCharset)))
    }
  }
}

private[riak] class RiakHttpClientHelper(system: ActorSystem) extends RiakUriSupport with RiakIndexSupport with DateTimeSupport {
  import scala.concurrent.Future
  import scala.concurrent.Future._
  import spray.http.{HttpEntity, HttpHeader, HttpResponse}
  import spray.http.StatusCodes._
  import spray.http.HttpHeaders._

  import org.slf4j.LoggerFactory

  import SprayClientExtras._
  import SprayJsonSupport._
  import RiakHttpHeaders._
  import RiakHttpClientHelper._

  import system.dispatcher

  private implicit val sys = system
  private val settings = RiakClientExtension(system).settings


  // ==========================================================================
  // Main HTTP Request Implementations
  // ==========================================================================

  def ping(server: RiakServerInfo): Future[Boolean] = {
    httpRequest(Get(PingUri(server))).map { response =>
      response.status match {
        case OK    => true
        case other => throw new OperationFailed(s"Ping on server '$server' produced an unexpected response code '$other'.")
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, key: String, resolver: RiakConflictsResolver): Future[Option[RiakValue]] = {
    httpRequest(Get(KeyUri(server, bucket, key))).flatMap { response =>
      response.status match {
        case OK              => successful(toRiakValue(response))
        case NotFound        => successful(None)
        case MultipleChoices => resolveConflict(server, bucket, key, response, resolver).map(Some(_))
        case other           => throw new BucketOperationFailed(s"Fetch for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case NotModified => successful(None)
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, index: RiakIndex, resolver: RiakConflictsResolver): Future[List[RiakValue]] = {
    httpRequest(Get(IndexUri(server, bucket, index))).flatMap { response =>
      response.status match {
        case OK         => fetchWithKeysReturnedByIndexLookup(server, bucket, response, resolver)
        case BadRequest => throw new ParametersInvalid(s"""Invalid index name ("${index.fullName}") or value ("${index.value}").""")
        case other      => throw new BucketOperationFailed(s"""Fetch for index "${index.fullName}" with value "${index.value}" in bucket "${bucket}" produced an unexpected response code: ${other}.""")
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, indexRange: RiakIndexRange, resolver: RiakConflictsResolver): Future[List[RiakValue]] = {
    httpRequest(Get(IndexRangeUri(server, bucket, indexRange))).flatMap { response =>
      response.status match {
        case OK         => fetchWithKeysReturnedByIndexLookup(server, bucket, response, resolver)
        case BadRequest => throw new ParametersInvalid(s"""Invalid index name ("${indexRange.fullName}") or range ("${indexRange.start}" to "${indexRange.start}").""")
        case other      => throw new BucketOperationFailed(s"""Fetch for index "${indexRange.fullName}" with range "${indexRange.start}" to "${indexRange.start}" in bucket "${bucket}" produced an unexpected response code: ${other}.""")
      }
    }
  }

  def store(server: RiakServerInfo, bucket: String, key: String, value: RiakValue, resolver: RiakConflictsResolver): Future[Unit] = {
    val request = createStoreHttpRequest(value)

    request(Put(KeyUri(server, bucket, key), value)).flatMap { response =>
      response.status match {
        case NoContent => successful(())
        case other     => throw new BucketOperationFailed(s"Store of value '$value' for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def storeAndFetch(server: RiakServerInfo, bucket: String, key: String, value: RiakValue, resolver: RiakConflictsResolver): Future[RiakValue] = {
    val request = createStoreHttpRequest(value)

    request(Put(KeyUri(server, bucket, key, StoreQueryParameters(true)), value)).flatMap { response =>
      response.status match {
        case OK              => successful(toRiakValue(response).getOrElse(throw new BucketOperationFailed(s"Store of value '$value' for key '$key' in bucket '$bucket' produced an unparsable reponse.")))
        case MultipleChoices => resolveConflict(server, bucket, key, response, resolver)
        case other           => throw new BucketOperationFailed(s"Store of value '$value' for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def delete(server: RiakServerInfo, bucket: String, key: String): Future[Unit] = {
    httpRequest(Delete(KeyUri(server, bucket, key))).map { response =>
      response.status match {
        case NoContent => ()
        case NotFound  => ()
        case other     => throw new BucketOperationFailed(s"Delete for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  def getBucketProperties(server: RiakServerInfo, bucket: String): Future[RiakBucketProperties] = {
    import spray.httpx.unmarshalling._

    httpRequest(Get(PropertiesUri(server, bucket))).map { response =>
      response.status match {
        case OK => response.entity.as[RiakBucketProperties] match {
          case Right(properties) => properties
          case Left(error)       => throw new BucketOperationFailed(s"Fetching properties of bucket '$bucket' failed because the response entity could not be parsed.")
        }
        case other => throw new BucketOperationFailed(s"Fetching properties of bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  def setBucketProperties(server: RiakServerInfo, bucket: String, newProperties: Set[RiakBucketProperty[_]]): Future[Unit] = {
    import spray.json._

    val entity = JsObject("props" -> JsObject(newProperties.map(property => (property.name -> property.json)).toMap))

    httpRequest(Put(PropertiesUri(server, bucket), entity)).map { response =>
      response.status match {
        case NoContent            => ()
        case BadRequest           => throw new ParametersInvalid(s"Setting properties of bucket '$bucket' failed because the http request contained invalid data.")
        case UnsupportedMediaType => throw new BucketOperationFailed(s"Setting properties of bucket '$bucket' failed because the content type of the http request was not 'application/json'.")
        case other                => throw new BucketOperationFailed(s"Setting properties of bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  def search(server: RiakServerInfo, bucket: String, solrQuery:RiakSearchQuery, resolver:RiakConflictsResolver): Future[RiakSearchResult] = {

    val query:Map[String, String] = solrQuery.m.toMap
    httpRequest(Get(SearchSolrUri(server, bucket, SolrQueryParameters(query)))).flatMap { response =>
      response.status match {
        case BadRequest => throw new ParametersInvalid(s"Invalid search or params (${solrQuery.m.toMap}) ")
        case OK         => successful(toRiakSearch(response, server, bucket, resolver))
        case other      => throw new BucketOperationFailed(s"Solr search '$query.toString' in bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  def createSearchIndex(server: RiakServerInfo, name:String, schema:String): Future[Boolean] = {

    httpRequest(Put(SearchIndexUri(server, name), JsObject("schema" -> JsString(schema)))).flatMap { response =>
      response.status match {
        case BadRequest => throw new ParametersInvalid(s"There was a problem creating the search index because the http request contained invalid data.")
        case OK         => successful(true)
        case other      => throw new BucketOperationFailed(s"There was a problem creating the search index '$other'.")
      }
    }
  }

  def getSearchIndex(server: RiakServerInfo, name:String): Future[Option[RiakSearchIndex]] = {
    httpRequest(Get(SearchIndexUri(server, name))).flatMap { response =>
      response.status match {
        case BadRequest => throw new ParametersInvalid(s"There was a problem getting the search index because the http request contained invalid data.")
        case OK         => parseSearchIndex(response.entity)
        case other      => throw new BucketOperationFailed(s"There was a problem creating the search index '$other'.")
      }
    }
  }

  /*def getSearchIndexList(server: RiakServerInfo, name:String): Future[List[RiakSearchIndex]] = {
    httpRequest(Get(SearchIndexUri(server, name))).flatMap { response =>
      response.status match {
        case BadRequest => throw new ParametersInvalid(s"There was a problem getting the search index because the http request contained invalid data.")
        case OK         => parseSearchIndex(response.entity)
        case other      => throw new BucketOperationFailed(s"There was a problem creating the search index '$other'.")
      }
    }
  }*/


  // ==========================================================================
  // Search utils
  // ==========================================================================

  private def parseSearchIndex(entity: HttpEntity): Future[Option[RiakSearchIndex]] = {
    import spray.httpx.unmarshalling._
    val searchResult = entity.as[RiakSearchIndex]
    val searchValue:Option[RiakSearchIndex] = if(searchResult.e.isRight) Some(searchResult.e.right.get) else None
    successful(searchValue)
  }

  /*private def parseSearchIndexList(entity:HttpEntity):Future[List[RiakSearchIndex]] = {

  }*/



  // ==========================================================================
  // Solr Building
  // ==========================================================================





  private def toRiakSearch(response: HttpResponse, server: RiakServerInfo, bucket: String, resolver:RiakConflictsResolver):RiakSearchResult = toRiakSearch(response.entity, response.headers, server, bucket, resolver)
  private def toRiakSearch(entity: HttpEntity, headers: List[HttpHeader], server: RiakServerInfo, bucket: String, resolver:RiakConflictsResolver):RiakSearchResult = {
    entity.toOption.map { body =>
      import spray.json._

      val responseHeader:JsObject =
        body.asString.parseJson.asJsObject.fields.get("responseHeader").get.asJsObject
      val response:JsObject =
        body.asString.parseJson.asJsObject.fields.get("response").get.asJsObject

      val responseObject = response.convertTo[RiakSearchResponse]

      //TODO: Fix double quote in string to avoid using replace
      val responseValues = RiakSearchValueResponse(
        values=responseObject.docs.map(x => fetch(server, bucket, x.id.replace("\"",""), resolver)))

      RiakSearchResult(
        response=responseObject,
        responseValues=responseValues,
        responseHeader=responseHeader.convertTo[RiakSearchResponseHeader],
        contentType=body.contentType,
        data=body.asString)
    }.get
  }


  // ==========================================================================
  // Request building
  // ==========================================================================

  private lazy val clientId = java.util.UUID.randomUUID().toString
  private val clientIdHeader = if (settings.AddClientIdHeader) Some(RawHeader(`X-Riak-ClientId`, clientId)) else None

  private def httpRequest:SendReceive = {
    addOptionalHeader(clientIdHeader) ~>
      addHeader("Accept", "*/*, multipart/mixed") ~>
      sendReceive
  }

  private def createStoreHttpRequest(value: RiakValue) = {
    val vclockHeader = value.vclock.toOption.map(vclock => RawHeader(`X-Riak-Vclock`, vclock))
    val indexHeaders = value.indexes.map(toIndexHeader(_)).toList

    addOptionalHeader(vclockHeader) ~>
      addHeaders(indexHeaders) ~>
      httpRequest
  }


  // ==========================================================================
  // Response => RiakValue
  // ==========================================================================

  private def toRiakValue(response: HttpResponse): Option[RiakValue] = toRiakValue(response.entity, response.headers)
  private def toRiakValue(entity: HttpEntity, headers: List[HttpHeader]): Option[RiakValue] = {
    entity.toOption.flatMap { body =>
      val vClockOption       = headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).map(_.value)
      val eTagOption         = headers.find(_.is("etag")).map(_.value)
      val lastModifiedOption = headers.find(_.is("last-modified")).map(h => dateTimeFromLastModified(h.asInstanceOf[`Last-Modified`]))
      val indexes            = toRiakIndexes(headers)

      for (vClock <- vClockOption; eTag <- eTagOption; lastModified <- lastModifiedOption)
      yield RiakValue(body.asString, body.contentType, vClock, eTag, fromSprayDateTime(lastModified), indexes)
    }
  }

  private def dateTimeFromLastModified(lm: `Last-Modified`): DateTime = toSprayDateTime(fromSprayDateTime(lm.date))
  private def lastModifiedFromDateTime(dateTime: DateTime): `Last-Modified` = `Last-Modified`(dateTime)


  // ==========================================================================
  // Index result fetching
  // ==========================================================================

  private def fetchWithKeysReturnedByIndexLookup(server: RiakServerInfo, bucket: String, response: HttpResponse, resolver: RiakConflictsResolver): Future[List[RiakValue]] = {
    response.entity.toOption.map { body =>
      import spray.json._

      val keys = body.asString.parseJson.convertTo[RiakIndexQueryResponse].keys

      traverse(keys)(fetch(server, bucket, _, resolver)).map(_.flatten)
    }.getOrElse(successful(Nil))
  }


  // ==========================================================================
  // Conflict Resolution
  // ==========================================================================

  private def resolveConflict(server: RiakServerInfo, bucket: String, key: String, response: HttpResponse, resolver: RiakConflictsResolver): Future[RiakValue] = {
    import spray.http._
    import spray.httpx.unmarshalling._

    val vclockHeader = response.headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).toList

    response.entity.as[MultipartContent](MultipartContentUnmarshaller) match {
      case Left(error) => throw new ConflictResolutionFailed(error.toString)
      case Right(multipartContent) => {
        // TODO: make ignoring deleted values optional

        val values = multipartContent.parts.filterNot(part => part.headers.exists(_.lowercaseName == `X-Riak-Deleted`.toLowerCase))
                                           .flatMap(part => toRiakValue(part.entity, vclockHeader ++ part.headers))
                                           .toSet

        // Store the resolved value back to Riak and return the resulting RiakValue
        val ConflictResolution(result, writeBack) = resolver.resolve(values)
        if (writeBack) {
          storeAndFetch(server, bucket, key, result, resolver)
        } else {
          successful(result)
        }
      }
    }
  }

  //Fix for avoiding errors when using unmarshal MultipartContent
  //TODO: Remove this when Spray fix the problem
  val mimeParsingConfig = {
    val config = new MIMEConfig
    config.setMemoryThreshold(-1) // use only in-memory parsing
    config
  }

  implicit val MultipartContentUnmarshaller = multipartContentUnmarshaller(`UTF-8`)
  def multipartContentUnmarshaller(defaultCharset: HttpCharset): Unmarshaller[MultipartContent] =
    multipartPartsUnmarshaller[MultipartContent](`multipart/*`, defaultCharset, MultipartContent(_))

  implicit val MultipartByteRangesUnmarshaller = multipartByteRangesUnmarshaller(`UTF-8`)
  def multipartByteRangesUnmarshaller(defaultCharset: HttpCharset): Unmarshaller[MultipartByteRanges] =
    multipartPartsUnmarshaller[MultipartByteRanges](`multipart/byteranges`, defaultCharset, MultipartByteRanges(_))

  def multipartPartsUnmarshaller[T <: MultipartParts](mediaRange: MediaRange,
                                                      defaultCharset: HttpCharset,
                                                      create: Seq[BodyPart] ⇒ T): Unmarshaller[T] =

    Unmarshaller[T](mediaRange) {
      case HttpEntity.NonEmpty(contentType, data) =>
        contentType.mediaType.parameters.get("boundary") match {
          case None | Some("") =>
            throw new Error("Content-Type with a multipart media type must have a non-empty 'boundary' parameter")
          case Some(boundary) =>
            val mimeMsg = new MIMEMessage(new ByteArrayInputStream(data.toByteArray), boundary, mimeParsingConfig)
            create(convertMimeMessage(mimeMsg, defaultCharset))
        }
      case HttpEntity.Empty ⇒ create(Nil)
    }

  def convertMimeMessage(mimeMsg: MIMEMessage, defaultCharset: HttpCharset): Seq[BodyPart] = {
    mimeMsg.getAttachments.asScala.map { part =>
      val rawHeaders: List[HttpHeader] =
        part.getAllHeaders.asScala.map(h => RawHeader(h.getName, h.getValue))(collection.breakOut)
      HttpParser.parseHeaders(rawHeaders) match {
        case (Nil, headers) =>
          val contentType = headers.mapFind { case `Content-Type`(t) => Some(t); case _ ⇒ None }
            .getOrElse(ContentType(`text/plain`, defaultCharset)) // RFC 2046 section 5.1
          val outputStream = new ByteArrayOutputStream
            FileUtils.copyAll(part.readOnce(), outputStream)
            BodyPart(HttpEntity(contentType, outputStream.toByteArray), headers)

        case (errors, headers) =>
          val contentType = headers.mapFind { case `Content-Type`(t) => Some(t); case _ ⇒ None }
            .getOrElse(ContentType(`text/plain`, defaultCharset)) // RFC 2046 section 5.1
          val outputStream = new ByteArrayOutputStream
            FileUtils.copyAll(part.readOnce(), outputStream)
            BodyPart(HttpEntity(contentType, outputStream.toByteArray), headers)
      }
    }(collection.breakOut)
  }
}
