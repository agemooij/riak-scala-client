/* See the file "LICENSE" for the full license governing this code. */

package com.scalapenos.riak

import com.scalapenos.riak.internal.SprayJsonSupport
import MediaTypes._

import scala.collection._
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.concurrent.Future


trait RiakSearchFormat{
  private[riak] val fmt:String
}

case class JSONSearchFormat() extends RiakSearchFormat {
  private[riak] val fmt:String = "json"
}

case class XMLSearchFormat() extends RiakSearchFormat {
  private[riak] val fmt:String = "xml"
}

case class RiakSearchQuery() {

  private[riak] val m:mutable.Map[String, String] = mutable.Map.empty

  def index(value:Option[String]) =
    if(value.nonEmpty) m("index") = value.get
    else m.remove("index")
  def q(value:Option[String]) =
    if(value.nonEmpty) m("q") = value.get
    else m.remove("q")
  def df(value:Option[String]) =
    if(value.nonEmpty) m("df") = value.get
    else m.remove("df")
  def q_op(value:Option[String]) =
    if(value.nonEmpty) m("q_op") = value.get
    else m.remove("q_op")
  def start(value:Option[Long]) =
    if(value.nonEmpty) m("q_op") = value.get.toString
    else m.remove("q_op")
  def rows(value:Option[Long]) =
    if(value.nonEmpty) m("rows") = value.get.toString
    else m.remove("rows")
  def sort(value:Option[Long]) =
    if(value.nonEmpty) m("sort") = value.get.toString
    else m.remove("sort")
  def wt(value:Option[RiakSearchFormat]) =
    if(value.nonEmpty) m("wt") = value.get.fmt
    else m.remove("wt")
  def filter(value:Option[String]) =
    if(value.nonEmpty) m("filter") = value.get
    else m.remove("filter")
  def presort(value:Option[String]) =
    if(value.nonEmpty) m("presort") = value.get
    else m.remove("presort")

  override def toString = s"${m.toString}"

}

 private[riak] trait RiakSearchJsonFormats {
   implicit val mapStringFormat : JsonFormat[Map[String, String]] = new JsonFormat[Map[String, String]] {
     //implementation
     def write(params: Map[String, String]) = {
       params.toJson
     }

     def read(value: JsValue) = {
       value.asJsObject.fields.toMap.mapValues(_ toString)
     }
   }

   implicit val RiakSolrSearchDocFormat : JsonFormat[List[RiakSearchDoc]] = new JsonFormat[List[RiakSearchDoc]] {
     //implementation
     def write(params: List[RiakSearchDoc]) = {
       params.map{
         x => JsObject(
         "id" -> JsString(x.id),
         "index" -> JsString(x.index),
         "fields" -> x.fields.mapValues(_ toString).toJson,
         "props" -> x.props.mapValues(_ toString).toJson)
       }.toJson
     }

     def read(value: JsValue) = {

       value.asInstanceOf[JsArray].elements.map{
         x => RiakSearchDoc(
           x.asJsObject.fields.get("id").get.toString,
           x.asJsObject.fields.get("index").get.toString,
           x.asJsObject.fields.get("fields").get.asJsObject.fields.toMap,
           x.asJsObject.fields.get("props").get.asJsObject.fields.toMap)
       }
     }
   }
}

private[riak] sealed case class RiakSearchDoc(
  id:String,
  index:String,
  fields:Map[String, Any],
  props:Map[String, Any]){


}

private[riak] sealed case class RiakSearchResponse(
  numFound:Int,
  start: Int,
  maxScore:String,
  docs:List[RiakSearchDoc])


private[riak] sealed case class RiakSearchValueResponse(
  values:List[Future[Option[RiakValue]]])

private[riak] object RiakSearchResponse extends RiakSearchJsonFormats{
  implicit val jsonFormat = jsonFormat4(RiakSearchResponse.apply)
}

private[riak] sealed case class RiakSearchResponseHeader(
  status:Int,
  QTime: Int,
  params:Map[String, String])

private[riak] object RiakSearchResponseHeader extends RiakSearchJsonFormats{
  implicit val jsonFormat = jsonFormat3(RiakSearchResponseHeader.apply)
}

case class RiakSearchResult(
  responseHeader:RiakSearchResponseHeader,
  response:RiakSearchResponse,
  responseValues:RiakSearchValueResponse,
  contentType:ContentType,
  data:String)

case class RiakSearchIndex(name:String, nVal:Int, schema:String)

object RiakSearchIndex {
  implicit object RiakSearchIndexFormat extends RootJsonReader[RiakSearchIndex] {
    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "n_val", "schema") match {
        case Seq(JsString(name), JsNumber(nVal), JsString(schema)) =>
          RiakSearchIndex (name, nVal.toInt, schema)
        case _ => throw new DeserializationException("riak search index json expected")
      }
    }
  }

  implicit object RiakSearchIndexListFormat extends RootJsonReader[List[RiakSearchIndex]] {
    def read(value: JsValue) = value.asInstanceOf[JsArray].elements.map(_.convertTo[RiakSearchIndex])
  }

}