ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val raspberry = (project in file("."))
  .settings(
    name := "raspberry"
  )

libraryDependencies += "org.virtuslab" % "scala-yaml_3" % "0.0.6"