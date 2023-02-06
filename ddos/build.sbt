ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

enablePlugins(AkkaGrpcPlugin)

lazy val ddos = (project in file("."))
  .settings(
    name := "ddos"
  )


val AkkaVersion = "2.7.0"

resolvers += "jitpack" at "https://jitpack.io"

//Add ScalaTest dependencies
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
    "org.reactivemongo" %% "reactivemongo" % "1.1.0-RC7"
)

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.32.0"

libraryDependencies += "com.github.Filocava99" % "TuSoW" % "0.8.3"

// https://mvnrepository.com/artifact/org.scalafx/scalafx
libraryDependencies += "org.scalafx" %% "scalafx" % "19.0.0-R30"