import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "aorra"
  val appVersion      = "1.0-SNAPSHOT"

  def crshVersion = "1.2.0"
  
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
    "org.crsh" % "crsh.shell.telnet" % crshVersion,
    "be.objectify" %% "deadbolt-java" % "2.0-SNAPSHOT"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Get rid of jBoss logging warning
    javaOptions := Seq("-Dorg.jboss.logging.provider=slf4j"),
    // Allows mock Http.Contexts to be built for play-authenticate
    libraryDependencies += "play" %% "play-test" % play.core.PlayVersion.current,
    // Debug tests
    //Keys.fork in (Test) := true,
    //javaOptions in (Test) += "-Xdebug",
    //javaOptions in (Test) += "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9998",
    resolvers += Resolver.url("play-easymail (release)", url("http://joscha.github.com/play-easymail/repo/releases/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns)
  ).dependsOn(RootProject(uri(
      "git://github.com/tjdett/play-authenticate.git#testing-code"))
  ).dependsOn(RootProject(uri(
      "git://github.com/schaloner/deadbolt-2-java.git"))
  ).dependsOn(RootProject(uri(
      "git://github.com/tjdett/play21-jackrabbit-plugin.git#shutdown"))
  ).dependsOn(RootProject(uri(
      "git://github.com/tjdett/play2-crash-plugin.git"))
  ).dependsOn(RootProject(uri(
      "git://github.com/uq-eresearch/aorra-graph-demo.git")))

}
