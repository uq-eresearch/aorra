// Comment to get more information during initialization
logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.1")

// Test coverage reporting
addSbtPlugin("de.johoop" % "jacoco4sbt" % "1.2.4")