name := "IterablePlayUtils"

version := "1.0"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  // Play! framework
  "com.typesafe.play" %% "play" % "2.2.2",

  // dependencies for tests
  "org.scalatest" %% "scalatest" % "2.1.5" % Test,
  "org.specs2" %% "specs2-mock" % "3.8.3" % Test
)

scalacOptions in Test ++= Seq("-Yrangepos")
