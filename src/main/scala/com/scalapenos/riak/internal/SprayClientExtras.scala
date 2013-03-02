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
package internal


private[riak] object SprayClientExtras {
  import spray.http.HttpHeader
  import spray.httpx.RequestBuilding.RequestTransformer

  def addOptionalHeader(header: => Option[HttpHeader]): RequestTransformer = _.mapHeaders(headers => header.toList ++ headers)
}


private[riak] object SprayJsonSupport extends spray.httpx.SprayJsonSupport {
  import spray.json._

  implicit object JsObjectFormat extends RootJsonFormat[JsObject] {
    def write(jsObject: JsObject) = jsObject
    def read(value: JsValue) = value.asJsObject
  }

  implicit object JsArrayFormat extends RootJsonFormat[JsArray] {
    def write(jsArray: JsArray) = jsArray
    def read(value: JsValue) = value.asInstanceOf[JsArray]
  }
}
