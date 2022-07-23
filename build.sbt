import Dependencies._

ThisBuild / organization := "com.mattzq"
ThisBuild / scalaVersion := "3.1.3"

ThisBuild / scalacOptions ++=
  Seq(
    "-deprecation",
    "-explain",
    "-feature",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Yexplicit-nulls", // experimental (I've seen it cause issues with circe)
    "-Ykind-projector",
    "-Ysafe-init", // experimental (I've seen it cause issues with circe)
  ) ++ Seq("-rewrite", "-indent") ++ Seq("-source", "future-migration")

lazy val `migrate` =
  project
    .in(file("."))
    .settings(name := "migrate")
    .settings(commonSettings)
    .settings(dependencies)

lazy val commonSettings = commonScalacOptions ++ Seq(
  update / evictionWarningOptions := EvictionWarningOptions.empty
)

lazy val commonScalacOptions = Seq(
  Compile / console / scalacOptions --= Seq(
    "-Wunused:_",
    "-Xfatal-warnings",
  ),
  Test / console / scalacOptions :=
    (Compile / console / scalacOptions).value,
)

val ZHTTPVersion = "2.0.0-RC9"

lazy val dependencies = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "2.0.0",
    "org.postgresql" % "postgresql" % "42.4.0",
  ),
  libraryDependencies ++= Seq(
    org.scalatest.scalatest,
    org.scalatestplus.`scalacheck-1-16`,
  ).map(_ % Test),
)

Compile / run / fork := true
