import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "aorra"
  val appVersion      = "1.0-SNAPSHOT"

  def jcloudsDep(s: String) = { "org.jclouds.api" % s % "1.5.7" }

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    jcloudsDep("filesystem"),
    jcloudsDep("swift")
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  )

}
