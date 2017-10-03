// Comment to get more information during initialization
logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.6")

// Release management
addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")
