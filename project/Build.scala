import sbt._
import Keys._
import play.Project._
import de.johoop.jacoco4sbt._
import JacocoPlugin._

object ApplicationBuild extends Build {

  val appName         = "aorra"
  val appVersion      = "1.0-SNAPSHOT"

  def crshVersion = "1.2.7"
  val BatikVersion = "1.7"

  val appDependencies = Seq(
    javaCore,
    "javax.jcr" % "jcr" % "2.0",
    "org.apache.jackrabbit" % "jackrabbit-core" % "2.6.2",
    "com.h2database" % "h2" % "1.3.170",
    "org.jcrom" % "jcrom" % "2.1.0"
      from "http://jcrom.googlecode.com/files/jcrom-2.1.0.jar",
    "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
    "com.google.guava" % "guava" % "14.0.1",
    "com.google.inject" % "guice" % "3.0",
    "org.crsh" % "crsh.shell.core" % crshVersion,
    "org.crsh" % "crsh.shell.telnet" % crshVersion,
    "org.crsh" % "crsh.jcr.jackrabbit" % crshVersion,
    "tyrex" % "tyrex" % "1.0.1", // JNDI provider for CRSH Jackrabbit access
    "com.feth" %% "play-authenticate" % "0.3.3-SNAPSHOT",
    "eu.medsea.mimeutil" % "mime-util" % "2.1.3"
      exclude("org.slf4j", "slf4j-log4j12"),
    "org.apache.tika" % "tika-parsers" % "1.3",
    "org.jsoup" % "jsoup" % "1.7.2" % "test",
    "com.icegreen" % "greenmail" % "1.3.1b" % "test",
    "org.jfree" % "jfreechart" % "1.0.14",
    "org.apache.xmlgraphics" % "batik-codec" % BatikVersion,
    "org.apache.xmlgraphics" % "batik-rasterizer" % BatikVersion,
    "org.apache.xmlgraphics" % "batik-svggen" % BatikVersion,
    "org.apache.xmlgraphics" % "fop" % "1.0",
    "org.apache.poi" % "poi" % "3.8",
    "org.apache.poi" % "poi-ooxml" % "3.8",
    "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "test",
    "com.kitfox.svg" % "svg-salamander" % "1.0",
    "net.sf.supercsv" % "super-csv" % "2.1.0",
    "org.apache.commons" % "commons-math3" % "3.2",
    "org.pegdown" % "pegdown" % "1.1.0",
    "hanjava.net" % "svg2emf" % "0.0.2",
    "org.freehep" % "freehep-graphicsio-emf" % "2.2.1",
    ("org.docx4j" % "docx4j" % "2.8.1" % "compile").intransitive(),
    "org.docx4j" % "xhtmlrenderer" % "1.0.0",
    "org.reflections" % "reflections" % "0.9.9-RC1"
  )

  val isTravisCI = System.getenv("TRAVIS_SCALA_VERSION") != null

  val coveralls = TaskKey[Unit]("coveralls", "Generate report file for Coveralls.io")

  lazy val s = Defaults.defaultSettings ++ Seq(jacoco.settings:_*)

  val main = play.Project(appName, appVersion, appDependencies, settings = s).settings(
    unmanagedResourceDirectories in Compile += file("resources"),
    requireJs ++= Seq("models.js") ,
    requireJsShim += "main.js",
    requireJsFolder += "js",
    // Show deprecation & feature warnings
    javacOptions += "-Xlint:deprecation",
    scalacOptions += "-feature",
    // Play Framework can't do parallel testing
    parallelExecution in jacoco.Config := false,
    // Jacoco report output
    jacoco.reportFormats in jacoco.Config := Seq(XMLReport("utf-8"), HTMLReport("utf-8"), CSVReport("utf-8")),
    jacoco.excludes in jacoco.Config := Seq("Route*", "Reverse*", "com*", "views*", "controllers.javascript.*"),
    jacoco.outputDirectory in jacoco.Config := file("target/jacoco"),
    // Coveralls
    coveralls := {
      println("Generating Coveralls.io JSON...")
      val out = new java.io.FileWriter("target/coveralls.json")
      out.write(CoverallJson.toString)
      out.close
    },
    javaOptions := Seq(
      // Get rid of jBoss logging warning
      "-Dorg.jboss.logging.provider=slf4j",
      // Silence "Graphics2D from BufferedImage lacks BUFFERED_IMAGE hint"
      "-Dorg.apache.batik.warn_destination=false",
      // Silence "org.docx4j.org.xhtmlrenderer.load INFO:: SAX XMLReader in use"
      "-Dxr.util-logging.java.util.logging.ConsoleHandler.level=OFF"
    ),
    // Allows mock Http.Contexts to be built for play-authenticate
    libraryDependencies += "play" %% "play-test" % play.core.PlayVersion.current,
    // Debug tests
    //Keys.fork in (Test) := true,
    //javaOptions in (Test) += "-Xdebug",
    //javaOptions in (Test) += "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9998",
    resolvers ++= Seq(
      // Specify central explicitly, so we don't try joscha.github.io unnecessarily
      "Maven central" at "http://repo1.maven.org/maven2/",
      // SVG2EMF
      "SVG2EMF" at "http://svg2emf.googlecode.com/svn/m2/releases/"
    ),
    resolvers += Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("play-authenticate (snapshot)", url("http://joscha.github.com/play-authenticate/repo/snapshots/"))(Resolver.ivyStylePatterns)
  ).dependsOn(RootProject(uri(
      "git://github.com/sgougi/play21-jackrabbit-plugin.git#996d969c088e4e2ca5d8b834aea0016fd3dd83b5"))
  ).dependsOn(RootProject(uri(
      "git://github.com/tjdett/play2-crash-plugin.git#41a02077133c51cf582320971b8d533c850f2a25")))

}
