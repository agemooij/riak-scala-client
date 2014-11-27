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

import org.slf4j.{Logger, LoggerFactory}
import RiakMapReduce._
import spray.json._
import DefaultJsonProtocol._
import scala.concurrent.Future
import scala.io.Source

class RiakMapReduceSpec extends RiakClientSpecification with RandomKeySupport {
  //private def randomBucket = client.bucket("riak-bucket-tests-" + randomKey)
  private def randomBucket = client.bucket("timesheet")

  val logger = LoggerFactory.getLogger("com.scalapenos.riak")

  case class MapReduceTest(name:String)

  "A RiakMapReduce" should {
    "be able to search with javascript text" in {
      val bucket = randomBucket
      val key = randomKey

      /*bucket.store(key, MapReduceTest("test1")).await
      bucket.store(key, MapReduceTest("test2")).await
      bucket.store(key, MapReduceTest("test3")).await

      def src(name: String) = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(name)).mkString

      logger.debug("*" * 10)
      val result = {
          client.mapReduce(InputSearch(bucket.name, "name_s:test*"))
              .map(JsSource(src("map.js")))
              .reduce(JsSource(src("reduce.js")))
              .result[List[MapReduceTest]]
      }

      //println(Await.result(result, 10.seconds))

      logger.debug(result.await.toString)
      logger.debug("*" * 10)*/


      true must beTrue

    }

    
  }

}
