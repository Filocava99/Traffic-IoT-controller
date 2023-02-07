ThisBuild / version := "0.1.0"
ThisBuild / organization := "it.unibo.pps.ddos"

name := "iot-controller"

scalaVersion := "3.2.2"

lazy val ddos = (project in file("ddos"))
lazy val raspberry = (project in file("raspberry")).dependsOn(ddos).aggregate(ddos)

lazy val root = (project in file("."))
  .aggregate(ddos, raspberry)