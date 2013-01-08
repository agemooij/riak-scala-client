package com.scalapenos.riak.utils

import spray.http.HttpHeader
import spray.httpx.RequestBuilding.RequestTransformer


object SprayClientExtras {
  def addOptionalHeader(header: => Option[HttpHeader]): RequestTransformer = _.mapHeaders(headers => header.toList ++ headers)
}
