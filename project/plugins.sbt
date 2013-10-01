resolvers ++= Seq(
  Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

addSbtPlugin("com.github.scct" % "sbt-scct" % "0.2")

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.0.0")
