resolvers ++= Seq(
  Classpaths.typesafeResolver,
  Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.1")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.2.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.0")

addSbtPlugin("com.orrsella" % "sbt-sublime" % "1.0.4")
