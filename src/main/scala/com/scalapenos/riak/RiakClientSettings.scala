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

import com.typesafe.config.Config


private[riak] class RiakClientSettings(config: Config) {

  /**
   * Setting for controlling whether the Riak client should add the
   * X-Riak-ClientId http header to all outgoing http requests.
   *
   * The value of the X-Riak-ClientId header will be UUID.randomUUID().toString
   * and will only be set once per instance of the RiakClientExtension (i.e.
   * per ActorSystem).
   *
   * This value defaults to true.
   */
  final val AddClientIdHeader: Boolean = config.getBoolean("riak.add-client-id-header")

}
