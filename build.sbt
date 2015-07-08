import com.typesafe.sbt.SbtScalariform
import scalariform.formatter.preferences._

lazy val formattingPreferences = FormattingPreferences().
  setPreference(AlignParameters, true).
  setPreference(DoubleIndentClassDeclaration, true)

lazy val formattingSettings = SbtScalariform.scalariformSettings ++ Seq(
  ScalariformKeys.preferences in Compile := formattingPreferences,
  ScalariformKeys.preferences in Test := formattingPreferences)

lazy val commonSettings = Seq(
  organization := "eu.shiftforward",
  version := "0.0.1",
  scalaVersion := "2.11.7",
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
) ++ formattingSettings

lazy val ridgeback = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "ridgeback",
    libraryDependencies ++= {
      val akkaV = "2.3.11"
      val sprayV = "1.3.3"
      val specs2V = "2.4.17"
      Seq(
        "io.spray"            %%  "spray-can"     % sprayV,
        "io.spray"            %%  "spray-routing" % sprayV,
        "io.spray"            %%  "spray-testkit" % sprayV  % "test",
        "io.spray"            %%  "spray-json"    % "1.3.2",
        "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
        "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
        "org.specs2"          %%  "specs2-core"   % specs2V % "test",
        "org.specs2"          %%  "specs2-mock"   % specs2V % "test",
        "com.typesafe.slick"  %%  "slick"         % "3.0.0",
        "com.h2database"      %   "h2"            % "1.4.187",
        "com.zaxxer"          %   "HikariCP-java6" % "2.3.8",
        "com.gettyimages"     %%  "spray-swagger" % "0.5.1"
      )
    }
  ) dependsOn moultingyamlProject

lazy val moultingyamlProject = RootProject(uri("git://github.com/jcazevedo/moultingyaml.git"))

Revolver.settings
