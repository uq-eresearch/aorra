// Comment to get more information during initialization
logLevel := Level.Warn

libraryDependencies += "net.liftweb" %% "lift-json" % "2.+"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.9")

// Test coverage reporting
addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.2")

// Release management
addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")
