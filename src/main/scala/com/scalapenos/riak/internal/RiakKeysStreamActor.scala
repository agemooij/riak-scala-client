package com.scalapenos.riak.internal

import akka.actor.{PoisonPill, ActorRef, ActorLogging, Actor}
import akka.io.IO
import play.api.libs.iteratee.{Input, Enumerator, Iteratee}
import spray.can.Http
import spray.http._
import spray.client.pipelining._
import spray.json.JsValue
import spray.json._
import DefaultJsonProtocol._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by mordonez on 11/28/14.
 */
class RiakKeysStreamActor(rq:HttpRequest) extends Actor with ActorLogging{

  val io = IO(Http)(context.system)

  val iteratee = Iteratee.fold[JsValue, JsObject](JsObject("keys" -> JsArray(List.empty[JsValue]:_*))){
    case (result, chunk) =>
      val newKeys = chunk.asJsObject.fields("keys") match {
        case JsArray(keys) => keys.map{
          case JsString(key) => key
          case _ => ""
        }
        case _ => List.empty[String]
      }
      val oldKeys = result.asJsObject().fields("keys") match {
        case JsArray(keys) => keys.map{
          case JsString(key) => key
          case _ => ""
        }
        case _ => List.empty[String]
      }

      val allKeys = (newKeys ++ oldKeys).toSeq.map(JsString(_))
      JsObject("keys" -> JsArray(allKeys:_*))
  }

  var internalIteratee:Option[Iteratee[JsValue, JsValue]] = Some(iteratee)

  var originalSender:Option[ActorRef] = None

  val iteratee2 = Iteratee.foreach[JsValue](s => println(s))

  val bla = for{
    i1 <-  iteratee
    i2 <-  iteratee2
  } yield i1

  def receive = {
    case "start" =>

      originalSender = Some(sender)

      context.become(waitForChunks)

      sendTo(io).withResponsesReceivedBy(self)(rq)

  }

  def waitForChunks:Actor.Receive = {
    case ChunkedResponseStart(res) => //println("start: " + res)

    case MessageChunk(body, ext) =>

      bla.feed(Input.El(body.asString.parseJson))

      /*val localIteratee = internalIteratee.get.feed(Input.El(body.asString.parseJson))
      localIteratee.onComplete{
        case scala.util.Success(chunkKeys) =>
          println(s"chunkKeys ${chunkKeys}")
        case scala.util.Failure(error) => println(s"chunkKeys error ${error}")
      }
      val newIteratee = Iteratee.flatten(localIteratee)
      internalIteratee = Some(newIteratee)*/

    case ChunkedMessageEnd(ext, trailer) =>
      println("end: " + ext)
      internalIteratee = Some(Iteratee.flatten(internalIteratee.get.feed(Input.EOF)))
      internalIteratee.get.run.onComplete {
        case scala.util.Success(allKeys) =>
          val keyAsList = allKeys.asJsObject.fields("keys") match {
            case JsArray(keys) => keys.map{
              case JsString(key) => key
              case _ => ""
            }
            case _ => List.empty[String]
          }
          //println("FIN ->", allKeys)
          originalSender.get ! keyAsList
      }

      self ! PoisonPill

    case _ =>
  }



}
