// ============================================================================
// Sonatype Deployment
// ============================================================================

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

publishTo <<= version { v =>
  val nexus = "http://oss.sonatype.org/"
  if (v.endsWith("-SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
