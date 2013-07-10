import de.johoop.jacoco4sbt._
import JacocoPlugin._

seq(jacoco.settings : _*)

seq(ScctPlugin.instrumentSettings : _*)
