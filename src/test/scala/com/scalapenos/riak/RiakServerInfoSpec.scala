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

import org.specs2.mutable._


class RiakServerInfoSpec extends Specification {
  "When constructing a RiakServerInfo, it" should {
    "accept just a host and a port" in {
      val info = RiakServerInfo("arthur", 4242)

      info.host mustEqual "arthur"
      info.port mustEqual 4242
      info.pathPrefix mustEqual ""
      info.useSSL should beFalse
    }

    "accept a host, a port, and a path prefix" in {
      val info = RiakServerInfo("ford", 9098, "prefect")

      info.host mustEqual "ford"
      info.port mustEqual 9098
      info.pathPrefix mustEqual "prefect"
      info.useSSL should beFalse
    }

    "accept a host, a port, a path prefix, and a boolean specifying the use of SSL" in {
      val info = RiakServerInfo("zaphod", 1066, "/Beeblebrox", true)

      info.host mustEqual "zaphod"
      info.port mustEqual 1066
      info.pathPrefix mustEqual "Beeblebrox"
      info.useSSL should beTrue
    }

    "accept a valid URL string (http)" in {
      val info = RiakServerInfo("http://trillian/")

      info.host mustEqual "trillian"
      info.port mustEqual 80
      info.pathPrefix mustEqual ""
      info.useSSL should beFalse
    }

    "accept a valid URL string (http + port)" in {
      val info = RiakServerInfo("http://trillian:1973/")

      info.host mustEqual "trillian"
      info.port mustEqual 1973
      info.pathPrefix mustEqual ""
      info.useSSL should beFalse
    }

    "accept a valid URL string (http + port + prefix)" in {
      val info = RiakServerInfo("http://marvin:2007/depressed")

      info.host mustEqual "marvin"
      info.port mustEqual 2007
      info.pathPrefix mustEqual "depressed"
      info.useSSL should beFalse
    }

    "accept a valid URL string (https + prefix)" in {
      val info = RiakServerInfo("https://marvin/depressed")

      info.host mustEqual "marvin"
      info.port mustEqual 443
      info.pathPrefix mustEqual "depressed"
      info.useSSL should beTrue
    }

    "accept a valid URL string (https + port + prefix)" in {
      val info = RiakServerInfo("https://marvin:2007/depressed")

      info.host mustEqual "marvin"
      info.port mustEqual 2007
      info.pathPrefix mustEqual "depressed"
      info.useSSL should beTrue
    }

    "fail on an invalid URL string" in {
      RiakServerInfo("server:2007/invalid") must throwA[java.net.MalformedURLException]
    }
  }
}
