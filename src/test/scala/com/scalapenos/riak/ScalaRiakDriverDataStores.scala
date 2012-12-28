package com.scalapenos.riak


class SprayClientRiakDriverDataStore() extends RiakDriverDataStore {
  def initializeRiakDriver: RiakDriver = SprayClientRiakDriver(system)
}
