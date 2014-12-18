package com.scalapenos.riak

import com.scalapenos.riak.internal.RiakChunkedMessage

/**
 * Created by mordonez on 12/18/14.
 */
private[riak] trait RiakStreamBase[T] {

  private[riak] var callbackOnChunkResponse:Option[RiakChunkedMessage[T] => Unit] = None
  private[riak] var callbackOnChunkFinish:Option[RiakChunkedMessage[T] => Unit] = None
  private[riak] var callbackOnError:Option[Throwable => Unit] = None

  def onChunk(f:RiakChunkedMessage[T] => Unit) = callbackOnChunkResponse = Some(f)
  def onFinish(f:RiakChunkedMessage[T] => Unit) = callbackOnChunkFinish = Some(f)
  def onError(f:Throwable => Unit) = callbackOnError = Some(f)

  private[riak] def setChunk(chunk:RiakChunkedMessage[T]) = if(callbackOnChunkResponse.nonEmpty) callbackOnChunkResponse.get(chunk)
  private[riak] def finish(chunk:RiakChunkedMessage[T]) = if(callbackOnChunkFinish.nonEmpty) callbackOnChunkFinish.get(chunk)
  private[riak] def timeoutError() = if(callbackOnError.nonEmpty) callbackOnError.get(OperationFailed("Timeout: server is not responding"))
}

private[riak] case class RiakStream[T]() extends RiakStreamBase[T]