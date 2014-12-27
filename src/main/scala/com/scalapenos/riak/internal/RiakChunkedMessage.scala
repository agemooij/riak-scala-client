package com.scalapenos.riak.internal

/**
 * Created by mordonez on 12/18/14.
 */
private[riak] trait RiakChunkedMessage[T] {
  def chunk:T
}
private[riak] trait RiakChunkedMessageStart[T] extends RiakChunkedMessage[T]{
}
private[riak] trait RiakChunkedMessageResponse[T] extends RiakChunkedMessage[T]{
}
private[riak] trait RiakChunkedMessageFinish[T] extends RiakChunkedMessage[T]{
}
