// Comment to get more information during initialization
logLevel := Level.Warn

libraryDependencies += "net.liftweb" %% "lift-json" % "2.+"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0")

// Test coverage reporting
addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.2")
