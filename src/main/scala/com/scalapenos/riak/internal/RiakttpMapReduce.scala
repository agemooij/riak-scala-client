package com.scalapenos.riak.internal

import com.scalapenos.riak.RiakMapReduce

/**
 * Created by mordonez on 11/25/14.
 */


private [riak] final class RiakHttpMapReduce(helper: RiakHttpClientHelper, server: RiakServerInfo, input: RiakMapReduce.Input) extends RiakMapReduce {
  import spray.json.RootJsonReader
  import com.scalapenos.riak.RiakMapReduce.QueryPhase
  def query[R: RootJsonReader](phases: Seq[(QueryPhase.Value, QueryPhase)]) = helper.mapReduce[R](server, input, phases)
}