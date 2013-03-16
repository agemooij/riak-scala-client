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

import scala.concurrent.duration._

import akka.actor.Props

import spray.routing._
import spray.http.MediaTypes._


object Main extends SimpleRoutingApp {
  // val repo = system.actorOf(Props[SimpleRiakUserRepositoryActor], "simple-riak-repo")

  startServer(interface = "localhost", port = 5001) {
    path("album" / PathElement) { title =>
      get {
        complete("not implemented yet")
      }
    } ~
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <h1>Welcome to the basic Riak Scala client Sample App</h1>
              <p>

              </p>
            </html>
          }
        }
      }
    } ~
    (post | parameter('method ! "post")) {
      path("stop") {
        complete {
          system.scheduler.scheduleOnce(1 second span) {
            system.shutdown()
          }
          "Shutting down in 1 second..."
        }
      }
    }
  }
}
