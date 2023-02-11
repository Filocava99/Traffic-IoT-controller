ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val ddos = RootProject(file("../ddos"))
lazy val raspbery = (project in file("."))
  .settings(
    name := "raspberry"
  ).dependsOn(ddos).aggregate(ddos)

libraryDependencies ++= Seq(
  "org.virtuslab" % "scala-yaml_3" % "0.0.6",
  "com.github.nscala-time" %% "nscala-time" % "2.32.0"
)