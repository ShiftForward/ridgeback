lazy val commonSettings = Seq(
  organization := "eu.shiftforward",
  version := "0.0.1",
  scalaVersion := "2.11.7",
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
)

lazy val ridgeback = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "ridgeback",
    libraryDependencies ++= {
      Seq(
        "org.yaml"            %   "snakeyaml"     % "1.15",
        "com.typesafe.slick"  %%  "slick"         % "3.0.0",
        "com.h2database"      %   "h2"            % "1.3.175"
        "com.zaxxer"          %   "HikariCP-java6" % "2.3.8",
        "com.gettyimages"     %%  "spray-swagger" % "0.5.1",
      )
    }
  )
