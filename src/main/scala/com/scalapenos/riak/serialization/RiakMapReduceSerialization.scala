package com.scalapenos.riak.serialization

import spray.json._
import com.scalapenos.riak.RiakMapReduce

trait RiakMapReduceSerialization {
  import com.scalapenos.riak.RiakMapReduce._
  implicit object RiakMapReduceQueryPhaseWriter extends RootJsonWriter[QueryPhase] {
    def write(obj: QueryPhase) = {
      var fields: Seq[(String, JsValue)] = obj.sourceFields map {
        case (key, value) ⇒ key → JsString(value)
      }
      fields :+= "language" → JsString("javascript")
      if (obj.keep) fields :+= "keep" → JsBoolean(true)
      JsObject(fields: _*)
    }
  }

  implicit object RiakMapReduceInputWriter extends JsonWriter[Input] {
    def write(obj: Input) = obj match {
      case input: RiakMapReduce.InputKeys    ⇒ JsArray(input.keys.map(k ⇒ JsArray(JsString(k._1), JsString(k._2))): _*)
      case input: RiakMapReduce.InputKeyData ⇒ JsArray(input.keys.map(k ⇒ JsArray(JsString(k._1), JsString(k._2), JsString(k._3))): _*)
      case input: RiakMapReduce.Input2i      ⇒ JsObject(List(
        "bucket" → JsString(input.bucket),
        "index" → JsString(input.index)
      ) ++ (input.value match {
        case Left(key) ⇒ List("key" → JsString(key))
        case Right((start, end)) ⇒ List("start" → JsNumber(start), "end" → JsNumber(end))
      }))
      case input: RiakMapReduce.InputSearch  ⇒ {
        var fields: List[JsField] = List(
          "bucket" → JsString(input.bucket),
          "query" → JsString(input.query)
        )
        input.filter.map(f ⇒ fields :+= "filter" → JsString(f))
        JsObject(fields: _*)
      }
      case input: RiakMapReduce.InputBucket  ⇒ input.keyFilters match {
        case Nil ⇒ JsString(input.bucket)
        case _   ⇒ JsObject(
          "bucket"      → JsString(input.bucket),
          "key_filters" → JsArray(input.keyFilters.map(keyFilter ⇒ JsArray(keyFilter.map(arg ⇒ JsString(arg)): _*)): _*)
        )
      }
    }
  }
}

object RiakMapReduceSerialization extends RiakMapReduceSerialization
