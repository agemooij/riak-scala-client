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

import java.util.zip.ZipException

import akka.actor._

import scala.util.Try

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

  import spray.client.pipelining._
  import spray.http.{ HttpEntity, HttpHeader, HttpResponse }
  import spray.http.StatusCodes._
  import spray.http.HttpHeaders._
  import spray.httpx.SprayJsonSupport._
  import spray.httpx.encoding.Gzip
  import spray.json.DefaultJsonProtocol._

  import SprayClientExtras._
  import RiakHttpHeaders._
  import RiakHttpClientHelper._

  import system.dispatcher

  private implicit val sys = system
  private val settings = RiakClientExtension(system).settings

  // ==========================================================================
  // Main HTTP Request Implementations
  // ==========================================================================

  def ping(server: RiakServerInfo): Future[Boolean] = {
    httpRequest(Get(PingUri(server))).map { response ⇒
      response.status match {
        case OK    ⇒ true
        case other ⇒ throw new OperationFailed(s"Ping on server '$server' produced an unexpected response code '$other'.")
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, key: String, resolver: RiakConflictsResolver): Future[Option[RiakValue]] = {
    httpRequest(Get(KeyUri(server, bucket, key))).flatMap { response ⇒
      response.status match {
        case OK              ⇒ successful(toRiakValue(response))
        case NotFound        ⇒ successful(None)
        case MultipleChoices ⇒ resolveConflict(server, bucket, key, response, resolver).map(Some(_))
        case other           ⇒ throw new BucketOperationFailed(s"Fetch for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case NotModified => successful(None)
      }
    }
  }

  def fetchWithSiblings(server: RiakServerInfo, bucket: String, key: String, resolver: RiakConflictsResolver): Future[Option[Set[RiakValue]]] = {
    httpRequest(Get(KeyUri(server, bucket, key))).flatMap { response ⇒
      response.status match {
        case OK              ⇒ successful(toRiakValue(response).map(Set(_)))
        case NotFound        ⇒ successful(None)
        case MultipleChoices ⇒ successful(Some(toRiakSiblingValues(response)))
        case other           ⇒ throw new BucketOperationFailed(s"Fetch for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, index: RiakIndex, resolver: RiakConflictsResolver): Future[List[RiakValue]] = {
    httpRequest(Get(IndexUri(server, bucket, index))).flatMap { response ⇒
      response.status match {
        case OK         ⇒ fetchWithKeysReturnedByIndexLookup(server, bucket, response, resolver)
        case BadRequest ⇒ throw new ParametersInvalid(s"""Invalid index name ("${index.fullName}") or value ("${index.value}").""")
        case other      ⇒ throw new BucketOperationFailed(s"""Fetch for index "${index.fullName}" with value "${index.value}" in bucket "${bucket}" produced an unexpected response code: ${other}.""")
      }
    }
  }

  def fetch(server: RiakServerInfo, bucket: String, indexRange: RiakIndexRange, resolver: RiakConflictsResolver): Future[List[RiakValue]] = {
    httpRequest(Get(IndexRangeUri(server, bucket, indexRange))).flatMap { response ⇒
      response.status match {
        case OK         ⇒ fetchWithKeysReturnedByIndexLookup(server, bucket, response, resolver)
        case BadRequest ⇒ throw new ParametersInvalid(s"""Invalid index name ("${indexRange.fullName}") or range ("${indexRange.start}" to "${indexRange.start}").""")
        case other      ⇒ throw new BucketOperationFailed(s"""Fetch for index "${indexRange.fullName}" with range "${indexRange.start}" to "${indexRange.start}" in bucket "${bucket}" produced an unexpected response code: ${other}.""")
      }
    }
  }

  def store(server: RiakServerInfo, bucket: String, key: String, value: RiakValue, resolver: RiakConflictsResolver): Future[Unit] = {
    val request = createStoreHttpRequest(value)

    request(Put(KeyUri(server, bucket, key), value)).flatMap { response ⇒
      response.status match {
        case NoContent ⇒ successful(())
        case other     ⇒ throw new BucketOperationFailed(s"Store of value '$value' for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def storeAndFetch(server: RiakServerInfo, bucket: String, key: String, value: RiakValue, resolver: RiakConflictsResolver): Future[RiakValue] = {
    val request = createStoreHttpRequest(value)

    request(Put(KeyUri(server, bucket, key, StoreQueryParameters(true)), value)).flatMap { response ⇒
      response.status match {
        case OK              ⇒ successful(toRiakValue(response).getOrElse(throw new BucketOperationFailed(s"Store of value '$value' for key '$key' in bucket '$bucket' produced an unparsable reponse.")))
        case MultipleChoices ⇒ resolveConflict(server, bucket, key, response, resolver)
        case other           ⇒ throw new BucketOperationFailed(s"Store of value '$value' for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
        // TODO: case PreconditionFailed => ... // needed when we support conditional request semantics
      }
    }
  }

  def delete(server: RiakServerInfo, bucket: String, key: String): Future[Unit] = {
    httpRequest(Delete(KeyUri(server, bucket, key))).map { response ⇒
      response.status match {
        case NoContent ⇒ ()
        case NotFound  ⇒ ()
        case other     ⇒ throw new BucketOperationFailed(s"Delete for key '$key' in bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  def getBucketProperties(server: RiakServerInfo, bucket: String): Future[RiakBucketProperties] = {
    import spray.httpx.unmarshalling._

    httpRequest(Get(PropertiesUri(server, bucket))).map { response ⇒
      response.status match {
        case OK ⇒ response.entity.as[RiakBucketProperties] match {
          case Right(properties) ⇒ properties
          case Left(error)       ⇒ throw new BucketOperationFailed(s"Fetching properties of bucket '$bucket' failed because the response entity could not be parsed.")
        }
        case other ⇒ throw new BucketOperationFailed(s"Fetching properties of bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  def setBucketProperties(server: RiakServerInfo, bucket: String, newProperties: Set[RiakBucketProperty[_]]): Future[Unit] = {
    import spray.json._

    val entity = JsObject("props" -> JsObject(newProperties.map(property ⇒ (property.name -> property.json)).toMap))

    // *Warning*: for some reason, Riak set bucket props HTTP endpoint doesn't handle compressed request properly.
    // Do not try to enable it here. Issue for tracking: https://github.com/agemooij/riak-scala-client/issues/41
    httpRequest(Put(PropertiesUri(server, bucket), entity)).map { response ⇒
      response.status match {
        case NoContent            ⇒ ()
        case BadRequest           ⇒ throw new ParametersInvalid(s"Setting properties of bucket '$bucket' failed because the http request contained invalid data.")
        case UnsupportedMediaType ⇒ throw new BucketOperationFailed(s"Setting properties of bucket '$bucket' failed because the content type of the http request was not 'application/json'.")
        case other                ⇒ throw new BucketOperationFailed(s"Setting properties of bucket '$bucket' produced an unexpected response code '$other'.")
      }
    }
  }

  // ==========================================================================
  // Unsafe bucket operations
  // ==========================================================================

  def allKeys(server: RiakServerInfo, bucket: String): Future[RiakKeys] = {
    import spray.httpx.unmarshalling._

    httpRequest(Get(AllKeysUri(server, bucket))).map { response ⇒
      response.status match {
        case OK ⇒ response.entity.as[RiakKeys] match {
          case Right(riakKeys) ⇒ riakKeys
          case Left(error)     ⇒ throw new BucketOperationFailed(s"List keys of bucket '$bucket' failed because the response entity could not be parsed.")
        }
        case other ⇒ throw new BucketOperationFailed(s"""List keys of bucket "${bucket}" produced an unexpected response code: ${other}.""")
      }
    }
  }

  // ==========================================================================
  // Request building
  // ==========================================================================

  private lazy val clientId = java.util.UUID.randomUUID().toString
  private val clientIdHeader = if (settings.AddClientIdHeader) Some(RawHeader(`X-Riak-ClientId`, clientId)) else None

  /**
   * Tries to decode gzipped response payload if response has an appropriate `Content-Encoding` header.
   * Returns the payload 'as is' if Gzip decoder throws a [[ZipException]].
   */
  private def safeDecodeGzip: ResponseTransformer = { response ⇒
    Try(decode(Gzip).apply(response)).recover {
      // recover from a ZipException: this means that, although the response has a "Content-Encoding: gzip" header, but its payload is not gzipped.
      case e: ZipException ⇒ response
    }.get
  }

  private def basePipeline(enableCompression: Boolean) = {
    if (enableCompression) {
      // Note that we don't compress request payload in here (e.g. using `encode(Gzip)` transformer).
      // This is due to a number of known shortcomings of Riak in regards to handling gzipped requests.
      addHeader(`Accept-Encoding`(Gzip.encoding)) ~> sendReceive ~> safeDecodeGzip
    } else {
      // So one might argue why would you need even to decode if you haven't asked for a gzip response via `Accept-Encoding` header? (the enableCompression=false case).
      // Well, there is a surprise from Riak: it will respond with gzip anyway if previous `store value` request was performed with `Content-Encoding: gzip` header! o_O
      // Yes, it's that weird...
      // And adding `addHeader(`Accept-Encoding`(NoEncoding.encoding))` directive for request will break it: Riak might respond with '406 Not Acceptable'
      // Issue for tracking: https://github.com/agemooij/riak-scala-client/issues/42
      sendReceive ~> safeDecodeGzip
    }
  }

  private def httpRequest = {
    addOptionalHeader(clientIdHeader) ~>
      addHeader("Accept", "*/*, multipart/mixed") ~>
      basePipeline(settings.EnableHttpCompression)
  }

  private def createStoreHttpRequest(value: RiakValue) = {
    val vclockHeader = value.vclock.toOption.map(vclock ⇒ RawHeader(`X-Riak-Vclock`, vclock))
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
    entity.toOption.flatMap { body ⇒
      val vClockOption = headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).map(_.value)
      val eTagOption = headers.find(_.is("etag")).map(_.value)
      val lastModifiedOption = headers.find(_.is("last-modified")).map(h ⇒ dateTimeFromLastModified(h.asInstanceOf[`Last-Modified`]))
      val indexes = toRiakIndexes(headers)

      for (vClock ← vClockOption; eTag ← eTagOption; lastModified ← lastModifiedOption)
        yield RiakValue(body.asString, body.contentType, vClock, eTag, lastModified, indexes)
    }
  }

  private def dateTimeFromLastModified(lm: `Last-Modified`): DateTime = fromSprayDateTime(lm.date)

  // ==========================================================================
  // Index result fetching
  // ==========================================================================

  private def fetchWithKeysReturnedByIndexLookup(server: RiakServerInfo, bucket: String, response: HttpResponse, resolver: RiakConflictsResolver): Future[List[RiakValue]] = {
    response.entity.toOption.map { body ⇒
      import spray.json._

      val keys = body.asString.parseJson.convertTo[RiakIndexQueryResponse].keys

      traverse(keys)(fetch(server, bucket, _, resolver)).map(_.flatten)
    }.getOrElse(successful(Nil))
  }

  // ==========================================================================
  // Conflict Resolution
  // ==========================================================================

  import spray.http._

  private def resolveConflict(server: RiakServerInfo, bucket: String, key: String, response: HttpResponse, resolver: RiakConflictsResolver): Future[RiakValue] = {
    import spray.httpx.unmarshalling._
    import FixedMultipartContentUnmarshalling._

    implicit val FixedMultipartContentUnmarshaller =
      // we always pass a Gzip decoder. Just in case if Riak decides to respond with gzip suddenly. o_O
      // Issue for tracking: https://github.com/agemooij/riak-scala-client/issues/42
      multipartContentUnmarshaller(HttpCharsets.`UTF-8`, decoder = Gzip)

    val vclockHeader = response.headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).toList

    response.entity.as[MultipartContent] match {
      case Left(error) ⇒ throw new ConflictResolutionFailed(error.toString)
      case Right(multipartContent) ⇒
        val bodyParts =
          if (settings.IgnoreTombstones)
            multipartContent.parts.filterNot(part ⇒ part.headers.exists(_.lowercaseName == `X-Riak-Deleted`.toLowerCase))
          else
            multipartContent.parts

        val values = bodyParts.flatMap(part ⇒ toRiakValue(part.entity, vclockHeader ++ part.headers)).toSet

        // Store the resolved value back to Riak and return the resulting RiakValue
        val ConflictResolution(result, writeBack) = resolver.resolve(values)
        if (writeBack) {
          storeAndFetch(server, bucket, key, result, resolver)
        } else {
          successful(result)
        }
    }
  }

  private def toRiakSiblingValues(response: HttpResponse): Set[RiakValue] = {
    import spray.http._
    import spray.httpx.unmarshalling._
    import FixedMultipartContentUnmarshalling._

    implicit val FixedMultipartContentUnmarshaller =
      // we always pass a Gzip decoder. Just in case if Riak decides to respond with gzip suddenly. o_O
      // Issue for tracking: https://github.com/agemooij/riak-scala-client/issues/42
      multipartContentUnmarshaller(HttpCharsets.`UTF-8`, decoder = Gzip)

    val vclockHeader = response.headers.find(_.is(`X-Riak-Vclock`.toLowerCase)).toList

    response.entity.as[MultipartContent] match {
      case Left(error) ⇒ throw new BucketOperationFailed(s"Failed to parse the server response as multipart content due to: '$error'")
      case Right(multipartContent) ⇒
        val bodyParts =
          if (settings.IgnoreTombstones)
            multipartContent.parts.filterNot(part ⇒ part.headers.exists(_.lowercaseName == `X-Riak-Deleted`.toLowerCase))
          else
            multipartContent.parts

        val values = bodyParts.flatMap(part ⇒ toRiakValue(part.entity, vclockHeader ++ part.headers)).toSet

        values
    }
  }
}

/**
 * Fix to work around the fact that Riak produces illegal Http content by not quoting ETag
 * headers in multipart/mixed responses. This breaks the Spray header parser so the below
 * class reproduces the minimal parts of Spray that are needed to provide a custom
 * unmarshaller for multipart/mixed responses.
 */
private[internal] object FixedMultipartContentUnmarshalling {
  import java.io.{ ByteArrayOutputStream, ByteArrayInputStream }
  import org.jvnet.mimepull.{ MIMEMessage, MIMEConfig }
  import org.parboiled.common.FileUtils
  import scala.collection.JavaConverters._
  import spray.http.parser.HttpParser
  import spray.httpx.encoding.Decoder
  import spray.httpx.unmarshalling._
  import spray.util._
  import spray.http._
  import MediaTypes._
  import HttpHeaders._

  def multipartContentUnmarshaller(defaultCharset: HttpCharset, decoder: Decoder): Unmarshaller[MultipartContent] =
    multipartPartsUnmarshaller[MultipartContent](`multipart/mixed`, defaultCharset, decoder, MultipartContent(_))

  private def multipartPartsUnmarshaller[T <: MultipartParts](mediaRange: MediaRange,
    defaultCharset: HttpCharset,
    decoder: Decoder,
    create: Seq[BodyPart] ⇒ T): Unmarshaller[T] =
    Unmarshaller[T](mediaRange) {
      case HttpEntity.NonEmpty(contentType, data) ⇒
        contentType.mediaType.parameters.get("boundary") match {
          case None | Some("") ⇒
            sys.error("Content-Type with a multipart media type must have a non-empty 'boundary' parameter")
          case Some(boundary) ⇒
            val mimeMsg = new MIMEMessage(new ByteArrayInputStream(data.toByteArray), boundary, mimeParsingConfig)
            create(convertMimeMessage(mimeMsg, defaultCharset, decoder))
        }
      case HttpEntity.Empty ⇒ create(Nil)
    }

  private def decompressData(headers: List[HttpHeader], decoder: Decoder, data: Array[Byte]): Array[Byte] = {
    // According to RFC (https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html),
    // "If multiple encodings have been applied to an entity, the content codings MUST be listed in the order in which
    // they were applied. Additional information about the encoding parameters MAY be provided by other entity-header
    // fields not defined by this specification."
    // This means that, if there were multiple encodings applied, this will NOT work.
    if (headers.findByType[`Content-Encoding`].exists(_.encoding == decoder.encoding)) {
      decoder.newDecompressor.decompress(data)
    } else {
      data // pass-through
    }
  }

  private def convertMimeMessage(mimeMsg: MIMEMessage, defaultCharset: HttpCharset, decoder: Decoder): Seq[BodyPart] = {
    mimeMsg.getAttachments.asScala.map { part ⇒
      val rawHeaders: List[HttpHeader] = part.getAllHeaders.asScala.map(h ⇒ RawHeader(h.getName, h.getValue))(collection.breakOut)

      // This is the custom code that detects illegal unquoted "Etag" headers and quotes them.
      val fixedHeaders = rawHeaders.map { header ⇒
        if (header.lowercaseName == "etag" && !header.value.startsWith('"')) RawHeader(header.name, "\"" + header.value + "\"")
        else header
      }

      HttpParser.parseHeaders(fixedHeaders) match {
        case (Nil, headers) ⇒
          val contentType = headers.mapFind { case `Content-Type`(t) ⇒ Some(t); case _ ⇒ None }
            .getOrElse(ContentType(`text/plain`, defaultCharset)) // RFC 2046 section 5.1
          val outputStream = new ByteArrayOutputStream
          FileUtils.copyAll(part.readOnce(), outputStream)

          val data = decompressData(headers, decoder, outputStream.toByteArray)
          BodyPart(HttpEntity(contentType, data), headers)

        case (errors, _) ⇒ sys.error("Multipart part contains %s illegal header(s):\n%s".format(errors.size, errors.mkString("\n")))
      }
    }(collection.breakOut)
  }

  private val mimeParsingConfig = {
    val config = new MIMEConfig
    config.setMemoryThreshold(-1) // use only in-memory parsing
    config
  }
}
