ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val ddos = (project in file("../ddos"))
lazy val raspberry = (project in file("../raspberry"))
lazy val client = (project in file("."))
  .settings(
      name := "client"
  ).dependsOn(ddos, raspberry).aggregate(ddos, raspberry)
