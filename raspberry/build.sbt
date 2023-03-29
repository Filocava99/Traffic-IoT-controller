ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val ddos = RootProject(file("../ddos"))
lazy val server = RootProject(file("../server"))
lazy val raspbery = (project in file("."))
  .settings(
      name := "raspberry",
      assembly / mainClass := Some("it.unibo.smartcity.raspberry.Main"),
      assembly / assemblyJarName := "raspberry.jar",
  ).dependsOn(ddos, server)//.aggregate(ddos, server)

libraryDependencies ++= Seq(
    "org.virtuslab" % "scala-yaml_3" % "0.0.6",
    "com.github.nscala-time" %% "nscala-time" % "2.32.0"
)

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