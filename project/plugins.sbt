resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.2")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0.BETA1")
