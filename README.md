
A fast, non-blocking idiomatic Scala client library for interacting with Riak.


## Current Status

***Preparing for a first public release***

This project is very new and still under heavy construction. The aim is to support
most, if not all, features exposed by the Riak HTTP API.

A first public release to the Sonatype/Central repositories is scheduled for February 2013.

So far, the following Riak API features are supported:

- Fetch
- Store
- Delete
- Customizable and strongly typed conflict resolution on all fetches (and Stores when returnbody=true)
- Automatic conversion between raw RiakValues and Scala (case) classes using type classes
- A RiakMeta[T] box type for retaining the riak meta data (like the vclock, the etag, the last modified date, the indexes, etc.) after conversion to Scala (case) classes.
- Secondary Indexes (2i): exact matches
- Secondary Indexes (2i): ranges
- auto-retry (a standard feature of the underlying spray-client library)

These Riak features are currently missing and/or under construction:

- link walking
- Map Reduce


## Design goals

- It should provide an idiomatic Scala client API
- It should be non-blocking (i.e. all calls are handled asynchronously and result in Futures)
- Easy Integration with Akka

The initial focus will be on supporting the Riak HTTP API. Protobuf support might be added
later but it has a low priority at the moment.


## Design and Implementation

The _riak-scala-client_ is based on [Akka] 2.1 and and [Spray] client 1.1.

The client is implemented as an Akka extension, making it very easy to use
from Akka-based applications and non-Akka applications alike.


## Why such a boring name?

It seems all the cool names (scalariak, riakka, riaktor, etc.) have been taken already.
If you come up with a cooler name, please let us know and eternal fame will be yours!


## License

The _riak-scala-client_ is licensed under [APL 2.0].


  [Akka]:    http://akka.io/
  [Spray]:    http://spray.io/
  [APL 2.0]: http://www.apache.org/licenses/LICENSE-2.0
