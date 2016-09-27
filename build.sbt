name := "IterablePlayUtils"

version := "1.0.1"

organization := "com.iterable"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  // Play! framework
  "com.typesafe.play" %% "play" % "2.2.2",

  // dependencies for tests
  "org.scalatest" %% "scalatest" % "2.1.5" % Test,
  "org.specs2" %% "specs2-mock" % "3.8.3" % Test
)

scalacOptions in Test ++= Seq("-Yrangepos")

parallelExecution in Test := false


// stuff for publishing

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/Iterable/iterable-play-utils"))

pomExtra := (
  <scm>
    <url>git@github.com:Iterable/iterable-play-utils.git</url>
    <connection>scm:git:git@github.com:Iterable/iterable-play-utils.git</connection>
  </scm>
  <developers>
    <developer>
      <id>Iterable</id>
      <name>Iterable</name>
      <url>https://iterable.com</url>
    </developer>
  </developers>
)
