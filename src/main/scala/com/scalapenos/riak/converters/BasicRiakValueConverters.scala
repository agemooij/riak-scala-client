/*
 * Copyright (C) 2012-2013 Age Mooij
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
package converters

import scala.util._


trait BasicRiakValueConverters {
  implicit def stringRiakValueConverter = new RiakValueConverter[String] {
    def contentType = ContentType.`text/plain`
    def serialize(string: String): String = string

    def read(riakValue: RiakValue): Try[String] = Success(riakValue.data)
  }
}

object BasicRiakValueConverters extends BasicRiakValueConverters
