// ============================================================================
// Sonatype Deployment
// ============================================================================

import SonatypeKeys._

sonatypeSettings

publishMavenStyle := true

pomIncludeRepository := { repo => true }

publishArtifact in Test := false

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
