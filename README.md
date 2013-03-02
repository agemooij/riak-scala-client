
## What is this?

A fast, non-blocking idiomatic Scala client library for interacting with [Riak].


## Current Status (March 2013): Preparing for public release

A first stable public version, 0.8, is scheduled to be released to the Sonatype/Central
repositories around mid-March 2013. We are currently adding the last finishing touches.

This project was started in December 2012 out of frustration about the (then) lack of non-blocking
Scala (or Java) client libraries for [Riak].


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

Other features include:

- An untyped RiakValue class for interacting with raw Riak values and their associated
  meta data (vlock, etag, content type, last modified time, indexes, etc.)
- A typed RiakMeta[T] class for interacting with deserialized values while retaining
  their associated meta data (vlock, etag, content type, last modified time, indexes, etc.)
- Customizable and strongly typed conflict resolution on all fetches (and stores when returnbody=true)
- Automatic (de)serialization of Scala (case) classes using type classes
    - builtin spray-json (de)serializers
- Automatic indexing of Scala (case) classes using type classes
- Auto-retry of fetches and stores (a standard feature of the underlying spray-client library)
- Completely non-blocking thanks to Scala 2.10 Futures, Akka, and spray-client
- Transparent integration with Akka projects through an Akka extension

The following Riak (http) API features are still under construction:

- Map Reduce
- link walking
- ping
- status
- listing all buckets
- listing all keys in a bucket
- Conditional fetch/store semantics (i.e. If-None-Match and If-Match for ETags and
  If-Modified-Since and If-Unmodified-Since for LastModified)

The riak-scala-client has been tested against [Riak] versions 1.2.x and 1.3.0.


## Design goals

- It should be non-blocking (i.e. all calls are handled asynchronously and result in Futures)
- It should not wrap the java client for Riak, since that only exposes a blocking API
- It should provide an idiomatic Scala client API without resorting to hard to learn DSLs
- It should integrate with Akka (using an Akka extension)
- It should be easy to use

The initial focus is on supporting the Riak HTTP API. Protobuf support might be added
later but it has a low priority at the moment.


## Design and Implementation

The _riak-scala-client_ is based on [Akka] 2.1 and and [Spray] client 1.1.

The client is implemented as an Akka extension, making it very easy to use
from Akka-based applications, with some wrapper code to make sure non-Akka
applications are not bothered by the underlying Akka layer.


## Why such a boring name?

It seems all the cool names, like riactive, riakka, riaktor, scalariak, etc. have already
been taken by other projects but there seems to be a common naming pattern used by client libraries
for other languages so that's what ended up deciding the name.

If you come up with a cooler name, please let us know and eternal fame will be yours!


## License

The _riak-scala-client_ is licensed under [APL 2.0].

  [Riak]:     http://basho.com/riak/
  [Akka]:     http://akka.io/
  [Spray]:    http://spray.io/
  [APL 2.0]:  http://www.apache.org/licenses/LICENSE-2.0
