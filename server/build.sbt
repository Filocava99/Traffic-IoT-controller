ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

Global / scalaVersion := "3.2.2"


lazy val ddos = RootProject(file("../ddos"))
lazy val raspberry = RootProject(file("../raspberry"))
lazy val server = (project in file("."))
  .settings(
    name := "server"
  ).dependsOn(ddos, raspberry).aggregate(ddos, raspberry)
