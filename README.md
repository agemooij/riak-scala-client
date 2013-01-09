
The intention of this new project is to become the default Scala Riak client.
It should support at least all the functionality of the Java driver while
providing an idiomatic, non-blocking Scala API.


## Design goals

- It should provide an idiomatic Scala client API
- It should be non-blocking (i.e. all calls are handled asynchronously and result in Futures)
- The API should be decoupled from any other library, so no Play2 or Scalaz iteratees as part
  of the exposed API

The initial focus will be on supporting the Riak HTTP API. Protobuf support might be added
later but it has a low priority at the moment.


## Design and Implementation

The _riak-scala-client_ is based on [Akka] 2.1 and and [Spray] client 1.1.


## License

The _riak-scala-client_ is licensed under [APL 2.0].


  [Akka]:    http://akka.io/
  [Spray]:    http://spray.io/
  [APL 2.0]: http://www.apache.org/licenses/LICENSE-2.0
