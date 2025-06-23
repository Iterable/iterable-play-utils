name := "IterablePlayUtils"
organization := "com.iterable"
homepage := Some(url("https://github.com/Iterable/iterable-play-utils"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/Iterable/iterable-play-utils"),
    "scm:git@github.com:Iterable/iterable-play-utils.git"
  )
)

licenses := Seq(
  "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
developers := List(
  Developer(
    id = "Iterable",
    name = "Iterable",
    email = "engineering@iterable.com",
    url = url("https://iterable.com")
  )
)

scalaVersion := "2.13.16"
crossScalaVersions := Seq("2.13.16")

val PlayVersion = "3.0.1"

libraryDependencies ++= Seq(
  // Play! framework
  "org.playframework" %% "play" % PlayVersion,
  "org.playframework" %% "play-joda-forms" % PlayVersion,
  // dependencies for tests
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

import org.typelevel.scalacoptions.ScalacOptions
Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement
Test / parallelExecution := false
Test / publishArtifact := false
pomIncludeRepository := (_ => false)

// Sonatype Central publishing configuration
ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
