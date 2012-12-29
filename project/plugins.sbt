resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo"
)

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.5.3")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.0")

