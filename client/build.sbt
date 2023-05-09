ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

Global / scalaVersion := "3.2.2"
val AkkaVersion = "2.7.0"

lazy val ddos = RootProject(file("../ddos"))
lazy val raspberry = RootProject(file("../raspberry"))
lazy val client = (project in file("."))
  .settings(
      name := "client"
  ).dependsOn(ddos, raspberry).aggregate(ddos, raspberry)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
)
libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.9.1"