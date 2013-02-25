name := "riak-scala-client"

version := "0.1-SNAPSHOT"

organization := "com.scalapenos.riak"

organizationHomepage := Some(url("http://scalapenos.com/"))

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
                  "Spray Nightlies"     at "http://nightlies.spray.io/")

libraryDependencies ++= {
  val akkaVersion  = "2.1.0"
  val sprayVersion = "1.1-20130123"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"             % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"             % akkaVersion,
    "io.spray"                %   "spray-client"           % sprayVersion,
    "io.spray"                %%  "spray-json"             % "1.2.3",
    "com.github.nscala-time"  %%  "nscala-time"            % "0.2.0",
    "ch.qos.logback"          %   "logback-classic"        % "1.0.9"        % "provided",
    "com.typesafe.akka"       %%  "akka-testkit"           % akkaVersion    % "test",
    "io.spray"                %   "spray-testkit"          % sprayVersion   % "test",
    "org.specs2"              %%  "specs2"                 % "1.14"         % "test"
  )
}

initialCommands in console += {
  List("import com.scalapenos.riak._", "import akka.actor._").mkString("\n")
}


// ============================================================================
// Sonatype Deployment
// ============================================================================

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://riak.scalapenos.com"))

publishArtifact in Test := false

publishMavenStyle := true

pomIncludeRepository := { repo => true }

pomExtra := (
  <scm>
    <url>git@github.com:agemooij/riak-scala-client.git</url>
    <connection>scm:git@github.com:agemooij/riak-scala-client.git</connection>
  </scm>
  <developers>
    <developer>
      <id>agemooij</id>
      <name>Age Mooij</name>
      <url>http://github.com/agemooij</url>
    </developer>
  </developers>
)

publishTo <<= version { v =>
  val nexus = "http://oss.sonatype.org/"
  if (v.endsWith("-SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

credentials ++= {
  val sonatype = ("Sonatype Nexus Repository Manager", "oss.sonatype.org")
  def loadMavenCredentials(file: java.io.File) : Seq[Credentials] = {
    xml.XML.loadFile(file) \ "servers" \ "server" map (s => {
      val host = (s \ "id").text
      val realm = if (host == sonatype._2) sonatype._1 else "Unknown"
      Credentials(realm, host, (s \ "username").text, (s \ "password").text)
    })
  }
  val ivyCredentials   = Path.userHome / ".ivy2" / ".credentials"
  val mavenCredentials = Path.userHome / ".m2"   / "settings.xml"
  (ivyCredentials.asFile, mavenCredentials.asFile) match {
    case (ivy, _) if ivy.canRead => Credentials(ivy) :: Nil
    case (_, mvn) if mvn.canRead => loadMavenCredentials(mvn)
    case _ => Nil
  }
}


// ============================================================================
// Sublime SBT config
// ============================================================================

sublimeExternalSourceDirectoryParent <<= baseDirectory { base =>
  base / ".sublime"
}

sublimeTransitive := true
