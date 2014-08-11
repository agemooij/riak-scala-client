import sbt.Keys._
import scala.Some

name := "riak-scala-client"

version := "0.9.1"

scalaVersion := "2.11.2"

organization := "com.scalapenos"

organizationHomepage := Some(url("http://scalapenos.com/"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://riak.scalapenos.com"))

scalacOptions := Seq("-encoding", "utf8",
                     "-target:jvm-1.7",
                     "-feature",
                     "-language:implicitConversions",
                     "-language:postfixOps",
                     "-unchecked",
                     "-deprecation",
                     "-Xlog-reflective-calls",
                     "-Ywarn-adapted-args",
		                 "-Xmax-classfile-name", "255"
                    )

resolvers ++= Seq("Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
                  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Spray Repository"    at "http://repo.spray.io/")

libraryDependencies ++= {
  val akkaVersion  = "2.3.2"
  val sprayVersion = "1.3.1"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"             % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"             % akkaVersion,
    "io.spray"                %%  "spray-client"           % sprayVersion,
    "io.spray"                %%  "spray-json"             % "1.2.6",
    "com.github.nscala-time"  %%  "nscala-time"            % "1.2.0",
    "ch.qos.logback"          %   "logback-classic"        % "1.1.2"        % "provided",
    "com.typesafe.akka"       %%  "akka-testkit"           % akkaVersion    % "test",
    "io.spray"                %%  "spray-testkit"          % sprayVersion   % "test",
    "org.specs2"              %%  "specs2-core"            % "2.3.11"       % "test"
  )
}

initialCommands in console += {
  List("import com.scalapenos.riak._", "import akka.actor._").mkString("\n")
}

net.virtualvoid.sbt.graph.Plugin.graphSettings