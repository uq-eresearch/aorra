import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "aorra"
  val appVersion      = "1.0-SNAPSHOT"

  def jcloudsDep(s: String) = { "org.jclouds.api" % s % "1.5.7" }

  val appDependencies = Seq(
    //jcloudsDep("filesystem"),
    //jcloudsDep("swift"),
    "javax.jcr" % "jcr" % "2.0",
    "org.apache.jackrabbit" % "jackrabbit-core" % "2.6.0",
    "com.h2database" % "h2" % "1.3.170"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  ).dependsOn(RootProject(uri(
      "git://github.com/tjdett/securesocial.git#master-module-code")))

}
