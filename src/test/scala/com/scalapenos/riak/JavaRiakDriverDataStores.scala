package com.scalapenos.riak


class BlockingJavaRiakDriverDataStore extends RiakDriverDataStore {
  def initializeRiakDriver: RiakDriver = new BlockingJavaRiakDriver()
}

class BlockingJavaProtobufRiakDriverDataStore extends RiakDriverDataStore {
  def initializeRiakDriver: RiakDriver = new BlockingJavaProtobufRiakDriver()
}

class NonBlockingJavaRiakDriverDataStore extends RiakDriverDataStore {
  def initializeRiakDriver: RiakDriver = new NonBlockingJavaRiakDriver()
}

class NonBlockingJavaProtobufRiakDriverDataStore extends RiakDriverDataStore {
  def initializeRiakDriver: RiakDriver = new NonBlockingJavaProtobufRiakDriver()
}
