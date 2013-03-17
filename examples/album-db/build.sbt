name := "riak-example-album-db"

version := "0.1-SNAPSHOT"

organization := "com.scalapenos"

scalaVersion := "2.10.1"

scalacOptions := Seq("-encoding", "utf8",
                     "-target:jvm-1.6",
                     "-feature",
                     "-language:implicitConversions",
                     "-language:postfixOps",
                     "-unchecked",
                     "-deprecation",
                     "-Xlog-reflective-calls",
                     "-Ywarn-adapted-args"
                    )

mainClass in Global := Some("com.scalapenos.riak.examples.albums.Main")


// ============================================================================
// Dependencies
// ============================================================================

resolvers ++= Seq("Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
                  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Spray Repository"    at "http://repo.spray.io/",
                  "Spray Nightlies"     at "http://nightlies.spray.io/")

libraryDependencies ++= {
  val akkaVersion       = "2.1.2"
  val sprayVersion      = "1.1-20130123"
  val riakClientVersion = "0.8-SNAPSHOT"
  Seq(
    "com.scalapenos"          %%  "riak-scala-client"  % riakClientVersion,
    "io.spray"                %   "spray-routing"      % sprayVersion,
    "io.spray"                %   "spray-can"          % sprayVersion,
    "ch.qos.logback"          %   "logback-classic"    % "1.0.10",
    "com.typesafe.akka"       %%  "akka-testkit"       % akkaVersion    % "test",
    "io.spray"                %   "spray-testkit"      % sprayVersion   % "test",
    "org.specs2"              %%  "specs2"             % "1.14"         % "test"
  )
}
