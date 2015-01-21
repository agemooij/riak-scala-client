import scalariform.formatter.preferences._

name := "riak-scala-client"

version := "0.9.5"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.11.5", "2.10.4")

crossVersion := CrossVersion.binary

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
                     "-Ywarn-adapted-args"
                    )

resolvers ++= Seq("Spray Repository" at "http://repo.spray.io/")
resolvers ++= Seq("Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases")
resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

libraryDependencies <++= (scalaVersion) { v: String =>
  val sprayVersion = "1.3.2"
  val akkaVersion = "2.3.9"
  val specs2Version = if (v.startsWith("2.10")) "2.4.15" else "2.4.15"
  Seq(
     "com.typesafe.akka"      %%  "akka-actor"        % akkaVersion,
     "com.typesafe.akka"      %%  "akka-slf4j"        % akkaVersion,
     "io.spray"               %%  "spray-client"      % sprayVersion,
     "io.spray"               %%  "spray-json"        % "1.3.1",
     "com.github.nscala-time" %%  "nscala-time"       % "1.6.0",
     "com.typesafe.akka"      %%  "akka-testkit"      % akkaVersion   % "test",
     "io.spray"               %%  "spray-testkit"     % sprayVersion  % "test",
     "org.specs2"             %%  "specs2"            % specs2Version % "test",
     "ch.qos.logback"         %   "logback-classic"   % "1.1.2"       % "provided"
  )
}

initialCommands in console += {
  List("import com.scalapenos.riak._", "import akka.actor._").mkString("\n")
}

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, false)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 90)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)
