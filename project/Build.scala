import sbt._
import sbtrelease.{ReleaseStep, releaseTask}
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys.releaseProcess
import sbtrelease.ReleaseStateTransformations._
import Keys._
import play.Project._
import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.universal.Keys.packageZipTarball

object ApplicationBuild extends Build {

  val appName         = "aorra"

  def crshVersion = "1.2.8"
  val PoiVersion = "3.9"
  val BatikVersion = "1.7"

  val appDependencies = Seq(
    javaCore,
    cache,
    "org.webjars" %% "webjars-play" % "2.2.1" exclude("org.scala-lang", "scala-library"),
    "org.webjars" % "requirejs" % "2.1.1",
    "org.webjars" % "underscorejs" % "1.5.2-2",
    "org.webjars" % "jquery" % "1.10.2-1",
    "org.webjars" % "q" % "0.9.7",
    "org.webjars" % "backbonejs" % "1.1.0",
    "org.webjars" % "backbone-localstorage" % "1.1.0",
    "org.webjars" % "momentjs" % "2.0.0",
    "org.webjars" % "spin-js" % "1.3.0",
    "org.webjars" % "typeaheadjs" % "0.9.3",
    "javax.jcr" % "jcr" % "2.0",
    "org.apache.jackrabbit" % "jackrabbit-core" % "2.7.1",
    "com.h2database" % "h2" % "1.3.174",
    "org.jcrom" % "jcrom" % "2.1.0",
    "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
    "com.google.guava" % "guava" % "15.0",
    "com.google.inject" % "guice" % "3.0",
    "org.crsh" % "crsh.shell.core" % crshVersion,
    "org.crsh" % "crsh.shell.telnet" % crshVersion,
    "org.crsh" % "crsh.jcr.jackrabbit" % crshVersion,
    "tyrex" % "tyrex" % "1.0.1", // JNDI provider for CRSH Jackrabbit access
    "com.feth" %% "play-authenticate" % "0.5.0-SNAPSHOT",
    "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2",
    "org.apache.tika" % "tika-parsers" % "1.3"
      exclude("org.apache.poi", "poi")
      exclude("org.apache.poi", "poi-ooxml")
      exclude("org.apache.poi", "poi-ooxml-schemas")
      exclude("org.apache.poi", "poi-scratchpad"),
    "org.apache.xmlbeans" % "xmlbeans" % "2.3.0", // POI dependency
    "org.jsoup" % "jsoup" % "1.7.2",
    "org.jfree" % "jfreechart" % "1.0.15",
    "org.apache.xmlgraphics" % "batik-codec" % BatikVersion,
    "org.apache.xmlgraphics" % "batik-rasterizer" % BatikVersion,
    "org.apache.xmlgraphics" % "batik-svggen" % BatikVersion,
    "org.apache.xmlgraphics" % "fop" % "1.0",
    //"org.apache.poi" % "poi" % PoiVersion,
    //"org.apache.poi" % "poi-ooxml" % PoiVersion,
    "org.eclipse.jetty" % "jetty-webapp" % "8.1.13.v20130916" % "test",
    "com.kitfox.svg" % "svg-salamander" % "1.0",
    "net.sf.supercsv" % "super-csv" % "2.1.0",
    "org.apache.commons" % "commons-math3" % "3.2",
    "org.pegdown" % "pegdown" % "1.1.0",
    "org.freehep" % "freehep-graphicsio-emf" % "2.2.1",
    ("org.docx4j" % "docx4j" % "2.8.1" % "compile").intransitive(),
    "org.docx4j" % "xhtmlrenderer" % "1.0.0",
    "org.reflections" % "reflections" % "0.9.9-RC1",
    "org.yaml" % "snakeyaml" % "1.13"
  )

  val isTravisCI = System.getenv("TRAVIS_SCALA_VERSION") != null

  val coveralls = TaskKey[Unit]("coveralls", "Generate report file for Coveralls.io")

  lazy val s = Defaults.defaultSettings ++
      releaseSettings ++
      play.Project.playJavaSettings

  val buildTarball: ReleaseStep =
    ReleaseStep(
      action = { st: State =>
        val extracted = Project.extract(st)
        val ref = extracted.get(thisProjectRef)
        extracted.runAggregated(packageZipTarball in Universal in ref, st)
      }
    )

  val main = play.Project(appName,
      dependencies = appDependencies, settings = s).settings(
    version <<= version in ThisBuild,
    unmanagedResourceDirectories in Compile += file("resources"),
    lessEntryPoints <<= (sourceDirectory in Compile)(base => (
      (base / "assets" / "stylesheets" * "*.less")
    )),
    // Produce scala object that knows the app version
    sourceGenerators in Compile <+= (sourceManaged in Compile, version) map { (dir, v) =>
      val file = dir / "helpers" / "AppVersion.scala"
      IO.write(file, s"""package helpers
          object AppVersion {
            override def toString = "$v"
          }""")
      Seq(file)
    },
    requireJs ++= Seq() ,
    requireJsShim += "main.js",
    requireJsFolder += "js",
    // Show deprecation & feature warnings
    javacOptions += "-Xlint:deprecation",
    scalacOptions += "-feature",
    javaOptions := Seq(
      // Get rid of jBoss logging warning
      "-Dorg.jboss.logging.provider=slf4j",
      // Silence "Graphics2D from BufferedImage lacks BUFFERED_IMAGE hint"
      "-Dorg.apache.batik.warn_destination=false",
      // Silence "org.docx4j.org.xhtmlrenderer.load INFO:: SAX XMLReader in use"
      "-Dxr.util-logging.java.util.logging.ConsoleHandler.level=OFF"
    ),
    // Allows mock Http.Contexts to be built for play-authenticate
    libraryDependencies +=
      "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current,
    // Debug tests
    //Keys.fork in (Test) := true,
    //javaOptions in (Test) += "-Xdebug",
    //javaOptions in (Test) += "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9998",
    resolvers ++= Seq(
      // Specify central explicitly, so we don't try joscha.github.io unnecessarily
      "Maven central" at "http://repo1.maven.org/maven2/",
      // Play Authenticate
      Resolver.url("play-easymail (release)", url("http://joscha.github.com/play-easymail/repo/releases/"))(Resolver.ivyStylePatterns),
      Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns),
      Resolver.url("play-authenticate (release)", url("http://joscha.github.com/play-authenticate/repo/releases/"))(Resolver.ivyStylePatterns),
      Resolver.url("play-authenticate (snapshot)", url("http://joscha.github.com/play-authenticate/repo/snapshots/"))(Resolver.ivyStylePatterns)
    ),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      buildTarball,
      setNextVersion,
      commitNextVersion
    )
  ).dependsOn(RootProject(uri(
      "git://github.com/sgougi/play21-jackrabbit-plugin.git#94d3d7f2bca50815b0c96d90a0f34d40440bda0f")))

}
