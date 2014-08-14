name := "riak-scala-client"

version := "0.9.5"

scalaVersion := "2.11.2"

organization := "com.scalapenos"

organizationHomepage := Some(url("http://scalapenos.com/"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://riak.scalapenos.com"))

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

resolvers ++= Seq("Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
                  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Spray Repository"    at "http://repo.spray.io/")

libraryDependencies ++= {
   val sprayVersion = "1.3.1"
   val akkaVersion = "2.3.4"
  Seq(
     "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
     "com.typesafe.akka"       %%  "akka-slf4j"             % akkaVersion,
     "io.spray" % "spray-client_2.11" % sprayVersion,
     "io.spray" %%  "spray-json" % "1.2.6",
     "com.github.nscala-time" %% "nscala-time" % "1.2.0",
     "ch.qos.logback"          %   "logback-classic"        % "1.1.1"        % "provided",
     "org.specs2" % "specs2_2.11" % "2.4",
     "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion
  )
}

initialCommands in console += {
  List("import com.scalapenos.riak._", "import akka.actor._").mkString("\n")
}
