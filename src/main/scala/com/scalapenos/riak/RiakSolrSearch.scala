/* See the file "LICENSE" for the full license governing this code. */

package com.scalapenos.riak

import scala.collection._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._


trait RiakSolrFormat{
  private[riak] val fmt:String
}

case class JSONSolrFormat() extends RiakSolrFormat {
  private[riak] val fmt:String = "json"
}

case class XMLSolrFormat() extends RiakSolrFormat {
  private[riak] val fmt:String = "xml"
}

case class RiakSolrQuery() {

  private[riak] val m:mutable.Map[String, String] = mutable.Map.empty

  def index(value:Option[String]) =
    if(!value.isEmpty) m("index") = value.get
    else m.remove("index")
  def q(value:Option[String]) =
    if(!value.isEmpty) m("q") = value.get
    else m.remove("q")
  def df(value:Option[String]) =
    if(!value.isEmpty) m("df") = value.get
    else m.remove("df")
  def q_op(value:Option[String]) =
    if(!value.isEmpty) m("q_op") = value.get
    else m.remove("q_op")
  def start(value:Option[Long]) =
    if(!value.isEmpty) m("q_op") = value.get.toString
    else m.remove("q_op")
  def rows(value:Option[Long]) =
    if(!value.isEmpty) m("rows") = value.get.toString
    else m.remove("rows")
  def sort(value:Option[Long]) =
    if(!value.isEmpty) m("sort") = value.get.toString
    else m.remove("sort")
  def wt(value:Option[RiakSolrFormat]) =
    if(!value.isEmpty) m("wt") = value.get.fmt
    else m.remove("wt")
  def filter(value:Option[String]) =
    if(!value.isEmpty) m("filter") = value.get
    else m.remove("filter")
  def presort(value:Option[String]) =
    if(!value.isEmpty) m("presort") = value.get
    else m.remove("presort")

  override def toString = s"${m.toString}"

}

 private[riak] trait RiakSolrSearchJsonFormats {
   implicit val mapStringFormat : JsonFormat[Map[String, String]] = new JsonFormat[Map[String, String]] {
     //implementation
     def write(params: Map[String, String]) = {
       params.toJson
     }

     def read(value: JsValue) = {
       value.asJsObject.fields.toMap.mapValues(_ toString)
     }
   }

   implicit val ListMapStringFormat : JsonFormat[List[RiakSolrSearchDoc]] = new JsonFormat[List[RiakSolrSearchDoc]] {
     //implementation
     def write(params: List[RiakSolrSearchDoc]) = {
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
         x => RiakSolrSearchDoc(
           x.asJsObject.fields.get("id").get.toString,
           x.asJsObject.fields.get("index").get.toString,
           x.asJsObject.fields.get("fields").get.asJsObject.fields.toMap,
           x.asJsObject.fields.get("props").get.asJsObject.fields.toMap)
       }
     }
   }
}

sealed case class RiakSolrSearchDoc(
  id:String,
  index:String,
  fields:Map[String, Any],
  props:Map[String, Any])

sealed case class RiakSolrSearchResponse(
  numFound:Int,
  start: Int,
  maxScore:String,
  docs:List[RiakSolrSearchDoc])

private[riak] object RiakSolrSearchResponse extends RiakSolrSearchJsonFormats{
  implicit val jsonFormat = jsonFormat4(RiakSolrSearchResponse.apply)
}

sealed case class RiakSolrSearchResponseHeader(
  status:Int,
  QTime: Int,
  params:Map[String, String])

private[riak] object RiakSolrSearchResponseHeader extends RiakSolrSearchJsonFormats{
  implicit val jsonFormat = jsonFormat3(RiakSolrSearchResponseHeader.apply)
}

case class RiakSolrResult(
  responseHeader:RiakSolrSearchResponseHeader,
  response:RiakSolrSearchResponse)

