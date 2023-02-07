ThisBuild / version := "0.1.0"
ThisBuild / organization := "it.unibo.pps.ddos"

name := "iot-controller"

scalaVersion := "3.2.2"

enablePlugins(AkkaGrpcPlugin)

ThisBuild / assemblyMergeStrategy := {
    case PathList("META-INF", _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
}
val AkkaVersion = "2.7.0"

resolvers += "jitpack" at "https://jitpack.io"

//Add ScalaTest dependencies
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
    "com.github.nscala-time" %% "nscala-time" % "2.32.0"
)

libraryDependencies += "com.github.Filocava99" % "TuSoW" % "0.8.3"

// https://mvnrepository.com/artifact/org.scalafx/scalafx
libraryDependencies += "org.scalafx" %% "scalafx" % "19.0.0-R30"

lazy val ddos = (project in file("ddos"))
lazy val raspberry = (project in file("raspberry")).dependsOn(ddos).aggregate(ddos)

lazy val root = (project in file("."))
  .aggregate(ddos, raspberry)