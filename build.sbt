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

lazy val ddos = RootProject(file("./ddos"))
lazy val raspberry = RootProject(file("./raspberry"))
lazy val client = RootProject(file("./client"))
lazy val server = RootProject(file("./server"))

lazy val root = (project in file("."))
  .aggregate(ddos, raspberry, client, server)

//lazy val utils = (project in file("utils"))
//  .settings(
//      assembly / assemblyJarName := "utils.jar",
//      // more settings here ...
//  )


ThisBuild / assemblyMergeStrategy := {
    case x if Assembly.isConfigFile(x) =>
        MergeStrategy.concat
    case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
        MergeStrategy.rename
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    //        (xs map {
    //            _.toLowerCase
    //        }) match {
    //            case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
    //                MergeStrategy.discard
    //            case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
    //                MergeStrategy.discard
    //            case "plexus" :: xs =>
    //                MergeStrategy.discard
    //            case "services" :: xs =>
    //                MergeStrategy.filterDistinctLines
    //            case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
    //                MergeStrategy.filterDistinctLines
    //            case _ => MergeStrategy.deduplicate
    //        }
    case _ => MergeStrategy.first
}