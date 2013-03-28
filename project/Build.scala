import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "aorra"
  val appVersion      = "1.0-SNAPSHOT"

  def crshVersion = "1.2.0-cr11"

  val appDependencies = Seq(
    javaCore,
    "javax.jcr" % "jcr" % "2.0",
    "org.apache.jackrabbit" % "jackrabbit-core" % "2.6.0",
    "com.h2database" % "h2" % "1.3.170",
    "org.jcrom" % "jcrom" % "2.0.0",
    "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
    "com.google.guava" % "guava" % "14.0.1",
    "com.google.inject" % "guice" % "3.0",
    "org.crsh" % "crsh.shell.core" % crshVersion,
    "org.crsh" % "crsh.shell.telnet" % crshVersion
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  ).dependsOn(RootProject(uri(
      "git://github.com/tjdett/securesocial.git#master-module-code"))
  ).dependsOn(RootProject(uri(
      "git://github.com/tjdett/play21-jackrabbit-plugin.git#shutdown")))

}
