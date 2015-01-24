
[![Build Status](https://api.travis-ci.org/agemooij/riak-scala-client.png?branch=master)](https://travis-ci.org/agemooij/riak-scala-client)
[![Coverage Status](https://coveralls.io/repos/agemooij/riak-scala-client/badge.png)](https://coveralls.io/r/agemooij/riak-scala-client)

## What is this?
An easy to use, non-blocking Scala client library for interacting with [Riak].

See the [project site] for full [documentation], [examples], [scaladocs], and more.

## Status (Jan 24th 2015)
The latest version is 0.9.5, which is compatible with [Akka] 2.3.9 and [Spray] 1.3.2.
It has been tested for compatibility against Riak 2.0.4

This library is cross-built against Scala 2.10.4 and 2.11.5 and compiled using
`-target:jvm-1.7`.

# Installation
To get started, add the following dependency to your SBT build:

    libraryDependencies += "com.scalapenos" %% "riak-scala-client" % 0.9.5

# Recent Changes

## 0.9.5
- Cross-built for Scala 2.10.4 and 2.11.5
- Upgraded to [Akka] 2.3.9 and [Spray] 1.3.2 (thanks to @CharlesAHunt)
- Upgraded the default JSON serialization implementation to [spray-json] 1.3.1, which is around 15 times faster than the 1.2.x versions we used before.
- Fully tested with [Riak] 2.0 (2.0.4) and 1.4 (1.4.10)
- Added Travis and Coveralls support (thanks to @zaneli)

## 0.9.0
- New feature: allow conflict resolvers to specify whether the resolved value should be written back to Riak or not (thanks to @asnare)
- Dropped support for older versions of [Akka] and/or [Spray]


## Features
So far, the following Riak (http) API features are supported:

- Fetch
- Store
- Delete
- Secondary Indexes (2i)
    - Fetching exact matches
    - Fetching ranges
    - Storing with indexes
- Getting/setting bucket properties
- ping

Other features include:

- Completely non-blocking thanks to Scala 2.10 Futures, Akka, and Spray
- Transparent integration with Akka projects through an Akka extension
- An untyped RiakValue class for interacting with raw Riak values and their associated
  meta data (vclock, etag, content type, last modified time, indexes, etc.)
- A typed RiakMeta[T] class for interacting with deserialized values while retaining
  their associated meta data (vclock, etag, content type, last modified time, indexes, etc.)
- Customizable and strongly typed conflict resolution on all fetches (and stores when returnbody=true)
- Automatic (de)serialization of Scala (case) classes using type classes
    - builtin spray-json (de)serializers
- Automatic indexing of Scala (case) classes using type classes
- Auto-retry of fetches and stores (a standard feature of the underlying spray-client library)

The following Riak (http) API features are not supported at this time:

- Link walking
- Map Reduce
- Listing all keys in a bucket
- Listing all buckets
- Conditional fetch/store semantics (i.e. If-None-Match and If-Match for ETags and
  If-Modified-Since and If-Unmodified-Since for LastModified)
- Node Status

The initial focus is on supporting the Riak HTTP API. Protobuf support might be added
later but it has a low priority at the moment.

The riak-scala-client has been tested against [Riak] versions 1.2.x, 1.3.x, 1.4.x, and 2.0.x


## Current Limitations

- 2i fetches do not use streaming when processing the initial Riak response containing
  all matching keys. This means that this list of keys matching the specified index
  is currently read into memory in its entirety. Fetches with large (100k+) result sets can
  be slow because of this and might potentially cause memory problems. A future release
  will solve this by streaming the data using Play iteratees.
- 2i index names and index values containing some special characters will not be handled
  correctly. This is due to the way these have to be encoded for transmission over HTTP.
  And earlier version did manual double URL encoding/decoding but that was not a
  sustainable solution. Please avoid using characters like ' ', ',', '?', '&', etc.
  in index names and index values for now.


## Why such a boring name?

It seems all the cool and appropriate names, like [riaktive], [riakka], [riaktor],
[scalariak], etc. have already been taken by other projects. But there seems to be a
common riak-xxx-client naming pattern used by Riak client libraries for other languages
so that's what ended up deciding the, admittedly boring, name.

If you come up with a cooler name, please let us know and eternal fame will be yours!


## License

The _riak-scala-client_ is licensed under [APL 2.0].

  [project site]:       http://riak.scalapenos.com/
  [documentation]:      http://riak.scalapenos.com/documentation.html
  [examples]:           http://riak.scalapenos.com/examples.html
  [scaladocs]:          http://riak.scalapenos.com/scaladocs/index.html#com.scalapenos.riak.package
  [Riak]:               http://basho.com/riak/
  [Akka]:               http://akka.io/
  [Spray]:              http://spray.io/
  [spray-json]          https://github.com/spray/spray-json/
  [APL 2.0]:            http://www.apache.org/licenses/LICENSE-2.0
  [riaktive]:           https://github.com/xaleraz/Riaktive
  [riakka]:             https://github.com/timperrett/riakka
  [riaktor]:            https://github.com/benmyles/riaktor
  [scalariak]:          https://github.com/ariejdl/scala-riak
