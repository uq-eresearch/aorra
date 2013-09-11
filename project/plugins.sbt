// Comment to get more information during initialization
logLevel := Level.Warn

libraryDependencies += "net.liftweb" %% "lift-json" % "2.+"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.4-RC2")

// Test coverage reporting
addSbtPlugin("de.johoop" % "jacoco4sbt" % "1.2.4")

// Dust client-side templating
addSbtPlugin("com.typesafe" % "play-plugins-dust" % "1.4.1-09122012")
