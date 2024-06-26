name := "IterablePlayUtils"
organization := "com.iterable"
homepage := Some(url("https://github.com/Iterable/iterable-play-utils"))
scmInfo := Some(
  ScmInfo(url("https://github.com/Iterable/iterable-play-utils"), "scm:git@github.com:Iterable/iterable-play-utils.git")
)

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
developers := List(
  Developer(id = "Iterable", name = "Iterable", email = "engineering@iterable.com", url = url("https://iterable.com"))
)

scalaVersion := "2.13.14"
crossScalaVersions := Seq(scalaVersion.value)

val PlayVersion = "3.0.1"

libraryDependencies ++= Seq(
  // Play! framework
  "org.playframework" %% "play" % PlayVersion,
  "org.playframework" %% "play-joda-forms" % PlayVersion,
  // dependencies for tests
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

parallelExecution in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at (nexus + "content/repositories/snapshots"))
  else
    Some("releases" at (nexus + "service/local/staging/deploy/maven2"))
}

publishArtifact in Test := false
pomIncludeRepository := (_ => false)

publishMavenStyle := true
publishTo := sonatypePublishToBundle.value

import ReleaseTransformations._
releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
