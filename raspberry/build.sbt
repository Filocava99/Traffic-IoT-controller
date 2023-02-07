ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val ddos = (project in file("../ddos"))
lazy val raspberry = (project in file("."))
  .settings(
    name := "raspberry"
  ).dependsOn(ddos).aggregate(ddos)

libraryDependencies ++= Seq(
  "org.virtuslab" % "scala-yaml_3" % "0.0.6",
  "org.reactivemongo" %% "reactivemongo" % "1.1.0-RC7"
)