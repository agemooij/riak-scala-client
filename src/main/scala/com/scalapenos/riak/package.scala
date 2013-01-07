package com.scalapenos

import spray.http.ContentType
import com.github.nscala_time.time.Imports._


package object riak {

  // ============================================================================
  // Value Classes
  // ============================================================================

  implicit class Vclock(val value: String) extends AnyVal {
    def isDefined = !isEmpty
    def isEmpty = value.isEmpty
  }

  object Vclock {
    val NotSpecified = new Vclock("")
  }


  // ============================================================================
  // RiakValue
  // ============================================================================

  // TODO: write a converter/serializer/marshaller RiakValue => T
  // TODO: write a converter/deserializer/unmarshaller T => RiakValue

  final case class RiakValue(
    value: Array[Byte],
    contentType: ContentType,
    vclock: Vclock,
    etag: String,
    lastModified: DateTime
    // links: Seq[RiakLink]
    // meta: Seq[RiakMeta]
  ) {

    def asString = new String(value, contentType.charset.nioCharset)

    // TODO: add as[T: RiakValueUnmarshaller] function linked to the ContentType

    // TODO: add common manipulation functions
  }

  object RiakValue {
    def apply(value: String): RiakValue = {
      val contentType = ContentType.`text/plain`

      new RiakValue(
        value.getBytes(contentType.charset.nioCharset),
        contentType,
        Vclock.NotSpecified,
        "",
        DateTime.now
      )
    }

    import spray.http.HttpBody
    import spray.httpx.marshalling._
    implicit val RiakValueMarshaller: Marshaller[RiakValue] = new Marshaller[RiakValue] {
      def apply(riakValue: RiakValue, ctx: MarshallingContext) {
        ctx.marshalTo(HttpBody(riakValue.contentType, riakValue.value))
      }
    }
  }


  // ============================================================================
  // Exceptions
  // ============================================================================

  case class BucketOperationFailed(cause: String) extends RuntimeException(cause)
  case class ConflictResolutionFailed(cause: String) extends RuntimeException(cause)
  case class ParametersInvalid(cause: String) extends RuntimeException(cause)

}