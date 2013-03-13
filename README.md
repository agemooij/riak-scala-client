
## What is this?

A fast, non-blocking Scala client library for interacting with [Riak].


## Current Status (March 2013): Preparing for public release

This project was started in December 2012 out of frustration about the (then) lack of non-blocking
Scala (or Java) client libraries for [Riak].

A first stable public version, 0.8, is scheduled to be released to the Sonatype/Central
repositories around mid-March 2013. We are currently adding the last finishing touches
and writing some proper documentation.


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

The following Riak (http) API features are still under construction:

- link walking
- Map Reduce
- status
- listing all buckets
- listing all keys in a bucket
- Conditional fetch/store semantics (i.e. If-None-Match and If-Match for ETags and
  If-Modified-Since and If-Unmodified-Since for LastModified)

The initial focus is on supporting the Riak HTTP API. Protobuf support might be added
later but it has a low priority at the moment.

The riak-scala-client has been tested against [Riak] versions 1.2.x and 1.3.0.


## Current Limitations

- 2i fetches do not use streaming when processing the initial Riak response containing
  all matching keys. This means that this list of keys matching the specified index
  is currently read into memory in its entirety. Fetches with large (100k+) result sets can
  be slow because of this and might potentially cause memory problems. A future release
  will solve this by streaming the key data using iteratees.


## Why such a boring name?

It seems all the cool and appropriate names, like [riaktive], [riakka], [riaktor],
[scalariak], etc. have already been taken by other projects. But there seems to be a
common riak-xxx-client naming pattern used by Riak client libraries for other languages
so that's what ended up deciding the, admittedly boring, name.

If you come up with a cooler name, please let us know and eternal fame will be yours!


## License

The _riak-scala-client_ is licensed under [APL 2.0].

  [Riak]:      http://basho.com/riak/
  [Akka]:      http://akka.io/
  [Spray]:     http://spray.io/
  [APL 2.0]:   http://www.apache.org/licenses/LICENSE-2.0
  [riaktive]:  https://github.com/xaleraz/Riaktive
  [riakka]:    https://github.com/timperrett/riakka
  [riaktor]:   https://github.com/benmyles/riaktor
  [scalariak]: https://github.com/ariejdl/scala-riak
