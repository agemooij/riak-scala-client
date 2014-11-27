package com.scalapenos.riak

import scala.concurrent.Future

/**
 * Created by mordonez on 11/25/14.
 */
trait RiakBucketType extends RiakBucketBasicProperties {

  def bucket(name:String, resolver: RiakConflictsResolver = DefaultConflictsResolver): RiakBucket

  def getProperties:Future[RiakBucketProperties]
  def setProperties(newProperties: Set[RiakBucketProperty[_]]):Future[Unit]

  def setSearchIndex(riakSearchIndex: RiakSearchIndex):Future[Boolean]

  override def toString = s"BucketType:$name"
}