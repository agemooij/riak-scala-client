import com.typesafe.startscript.StartScriptPlugin

name := "riak-scala-client"

version := "0.1-SNAPSHOT"

organization := "Scalapenos"

scalaVersion := "2.10.0"

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
                  "Spray Repository"    at "http://repo.spray.io/",
                  "Spray Nightlies"     at "http://nightlies.spray.io/"
                 )

libraryDependencies ++= {
  val akkaVersion  = "2.1.0"
  val sprayVersion = "1.1-20121228"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"             % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"             % akkaVersion,
    "io.spray"                %   "spray-client"           % sprayVersion,
    "io.spray"                %%  "spray-json"             % "1.2.3",
    "com.github.nscala-time"  %%  "nscala-time"            % "0.2.0",
    "ch.qos.logback"          %   "logback-classic"        % "1.0.9"        % "runtime",
    "com.typesafe.akka"       %%  "akka-testkit"           % akkaVersion    % "test",
    "io.spray"                %   "spray-testkit"          % sprayVersion   % "test",
    "org.specs2"              %%  "specs2"                 % "1.13"         % "test"
  )
}

parallelExecution in Test := false
