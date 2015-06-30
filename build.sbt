val snakeyaml = "org.yaml" % "snakeyaml" % "1.15"

lazy val commonSettings = Seq(
  organization := "eu.shiftforward",
  version := "0.0.1",
  scalaVersion := "2.11.7"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "ridgeback",
    libraryDependencies += snakeyaml
  )
