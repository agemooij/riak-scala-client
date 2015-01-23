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

import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import org.specs2.specification.BeforeAfterExample
import spray.json.DefaultJsonProtocol._
import spray.json.JsString
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.higherKinds

class RiakSearchSpec extends RiakClientSpecification with RandomKeySupport {


  trait delayedBefore extends org.specs2.mutable.Before {
    def before =
      Thread.sleep(5000)
  }

  trait delayedAfter extends org.specs2.mutable.After {
    def after =
      Thread.sleep(5000)
  }

  case class SongTestComplex (number_i: Int, title_s: String, data_b:Map[String, String])
  object SongTestComplex {
    implicit val jsonFormat = jsonFormat3(SongTestComplex.apply)
  }

  val logger = LoggerFactory.getLogger("com.scalapenos.riak")

  val randomIndex = randomKey

  val bucket = client.bucket("riak-bucket-tests-" + randomKey)
  val schemaName = randomKey.split("-").mkString("").substring(0, 5)

  sequential

  "A RiakClient" should {

    "create a search index" in new delayedAfter{
      pending
      val newIndex =  client.createSearchIndex(randomIndex).await
      newIndex should beAnInstanceOf[RiakSearchIndex]
    }

    "get a list of all search index" in {
      pending
      client.getSearchIndexList.await must beAnInstanceOf[List[RiakSearchIndex]].eventually
    }

    "get an index by name" in new delayedBefore {
      pending
      val getIndex = {
        client.getSearchIndex(randomIndex).await
      }
      getIndex should beAnInstanceOf[RiakSearchIndex]
    }

    "assign a search index to a bucket, insert two elements and search with solr to get them back" in {
      pending
      val riakIndex = client.getSearchIndex(randomIndex).await
      val indexAssigned = (bucket.setSearchIndex(riakIndex)).await

      val songComplex1 = SongTestComplex(1, "titulo1", Map("test1" -> "datatest1"))
      bucket.store(s"$randomKey-song1", songComplex1).await

      val songComplex2 = SongTestComplex(2, "titulo2", Map("test2" -> "datatest2"))
      bucket.store(s"$randomKey-song2", songComplex2).await

      Thread.sleep(5000)

      val solrQuery = RiakSearchQuery()
      solrQuery.wt(Some(JSONSearchFormat()))
      solrQuery.q(Some("title_s:titulo*"))


      val query = client.getSearchIndex(randomIndex).flatMap( x => client.search(x, solrQuery) ).await

      query should beAnInstanceOf[RiakSearchResult]

    }

    "throw an error when delete a search index assigned to a bucket" in {
      pending
      client.deleteSearchIndex(randomIndex).await must throwA[Exception]
    }

    "delete a search index" in {
      pending
      (bucket.setSearchIndex(RiakNoSearchIndex)).await
      client.deleteSearchIndex(randomIndex).await must beTrue

    }

    "create a schema" in new delayedAfter{
      pending
      val schema =
        <schema name="schedule" version="1.5">
          <fields>
            <field name="_yz_id"   type="_yz_str" indexed="true" stored="true"  multiValued="false" required="true"/>
            <field name="_yz_ed"   type="_yz_str" indexed="true" stored="false" multiValued="false"/>
            <field name="_yz_pn"   type="_yz_str" indexed="true" stored="false" multiValued="false"/>
            <field name="_yz_fpn"  type="_yz_str" indexed="true" stored="false" multiValued="false"/>
            <field name="_yz_vtag" type="_yz_str" indexed="true" stored="false" multiValued="false"/>
            <field name="_yz_rk"   type="_yz_str" indexed="true" stored="true"  multiValued="false"/>
            <field name="_yz_rt"   type="_yz_str" indexed="true" stored="true"  multiValued="false"/>
            <field name="_yz_rb"   type="_yz_str" indexed="true" stored="true"  multiValued="false"/>
            <field name="_yz_err"  type="_yz_str" indexed="true" stored="false" multiValued="false"/>
          </fields>
          <uniqueKey>_yz_id</uniqueKey>
          <types>
            <fieldType name="_yz_str" class="solr.StrField" sortMissingLast="true" />
          </types>
        </schema>;


      client.createSearchSchema(schemaName, schema).await must beTrue
    }

    "get a schema" in new delayedBefore {
      pending
      client.getSearchSchema(schemaName).await should beAnInstanceOf[scala.xml.Elem]
    }

    "test" in {
      val bucket = client.bucket("timesheet")

      val solrQuery = RiakSearchQuery()
      solrQuery.wt(Some(JSONSearchFormat()))
      solrQuery.q(Some("id:*"))
      solrQuery.rows(Some(9999))
      solrQuery.presort(Some("key"))
      solrQuery.start(Some(0))

      val resultSearch:RiakSearchResult = bucket.search(solrQuery).await

      var result = 0

      def loadFuturesSequentally[A, B, C[A] <: Iterable[A]](collection:C[A])(fn:A => Future[B])(implicit exec:ExecutionContext, cbf:CanBuildFrom[C[B], B, C[B]]):Future[C[B]] = {
        val builder = cbf()
        builder.sizeHint(collection.size)

        collection.foldLeft(Future(builder)){
          (previousFuture, nextFuture) =>
            for{
              previousResult <- previousFuture
              nextResult <- fn(nextFuture)
            } yield {
              result += 1
              println(result)
              previousResult += nextResult
            }
        } map { builder => builder.result }
      }

      def getItem(item:RiakSearchDoc) = {
        bucket.fetch(item._yz_rk.replace("\"",""))
      }

      val startSearch = System.nanoTime

      val dataResult = loadFuturesSequentally(resultSearch.response.docs)(getItem).await

      println("total time : %s".format((System.nanoTime() - startSearch) / 1000))

      println(dataResult.size)

      dataResult should beAnInstanceOf[List[RiakValue]].eventually(1, 10 minutes)
    }

  }

}