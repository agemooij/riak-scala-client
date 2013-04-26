package com.scalapenos.riak

import spray.json.DefaultJsonProtocol
import org.specs2.mutable.After

class RiakLinkSpec extends RiakClientSpecification with RandomKeySupport {
  val bucketName = "riak-client-test" + randomKey

  case class Entity(id: String, name: String, parent: String)

  implicit object EntityRiakSupport extends RiakSerializer[Entity] with RiakDeserializer[Entity] with RiakLinker[Entity] with DefaultJsonProtocol {
    import spray.json._
    implicit val entityFormat = jsonFormat3(Entity)

    def serialize(t: Entity) = t.toJson.toString() → ContentType.`application/json`
    def deserialize(data: String, contentType: ContentType) = data.asJson.convertTo[Entity]
    def links(value: Entity) = Set(new RiakLink(bucketName, value.parent, "parent"))
  }

  "A Riak Link" should {
    "store and fetch a value with a link" in new After {
      val bucket = client.bucket(bucketName)
      val entity = Entity("1", "one", "0")
      val value = bucket.storeAndFetch("e1", entity).await
      value.links.toList.filter(_.tag.isDefined) must beLike {
        case List(RiakLink(_@bucketName, Some("0"), Some(("riaktag", "parent")))) ⇒ ok
      }

      def after = {
        client.bucket(bucketName).delete("e1").await
      }
    }
  }
}
