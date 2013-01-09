
The intention of this new project is to become the default Scala Riak client.
It should support at least all the functionality of the Java driver.


## Design goals

- Provide an idiomatic Scala client API
- It should be non-blocking (i.e. all calls are handled asynchronously and result in Futures)


## Design and Implementation

The _riak-scala-client_ is based on [Akka] and [Spray].


## License

_riak-scala-client_ is licensed under [APL 2.0].


  [Akka]:    http://akka.io/
  [Spray]:    http://spray.io/
  [APL 2.0]: http://www.apache.org/licenses/LICENSE-2.0
