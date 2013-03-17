resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo",
  Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

addSbtPlugin("reaktor" % "sbt-scct" % "0.2-SNAPSHOT")
