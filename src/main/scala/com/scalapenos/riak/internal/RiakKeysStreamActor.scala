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

  var originalSender:Option[ActorRef] = None


  def receive = {
    case "start" =>

      originalSender = Some(sender)

      context.become(waitForChunks)

      sendTo(io).withResponsesReceivedBy(self)(rq)

  }

  def waitForChunks:Actor.Receive = {
    case ChunkedResponseStart(res) =>

      originalSender.get ! new RiakChunkedMessageStart[List[String]]{
        def chunk = List.empty[String]
      }
      //println("start: " + res)

    case MessageChunk(body, ext) =>

      val keyAsList = body.asString.parseJson.asJsObject.fields("keys") match {
        case JsArray(keys) => keys.map{
          case JsString(key) => key
          case _ => ""
        }
        case _ => List.empty[String]
      }
      originalSender.get ! new RiakChunkedMessageResponse[List[String]]{
        def chunk = keyAsList
      }

    case ChunkedMessageEnd(ext, trailer) =>
      //println("end: " + ext)
      originalSender.get ! new RiakChunkedMessageFinish[List[String]]{
        def chunk = List.empty[String]
      }

      self ! PoisonPill

    case _ =>
  }



}
