import com.typesafe.sbt.SbtScalariform
import scalariform.formatter.preferences._
import NativePackagerKeys._

packageArchetype.java_application

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

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val ridgeback = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "ridgeback",
    libraryDependencies ++= {
      val akkaV = "2.3.12"
      val sprayV = "1.3.3"
      val specs2V = "2.4.17"
      Seq(
        "ch.qos.logback"      %   "logback-classic"  % "1.1.3",
        "com.gettyimages"     %%  "spray-swagger"    % "0.5.1",
        "com.h2database"      %   "h2"               % "1.4.187",
        "com.pusher"          %   "pusher-http-java" % "0.9.3",
        "com.typesafe.akka"   %%  "akka-actor"       % akkaV,
        "com.typesafe.akka"   %%  "akka-testkit"     % akkaV   % "test",
        "com.typesafe.slick"  %%  "slick"            % "3.0.0",
        "com.zaxxer"          %   "HikariCP-java6"   % "2.3.9",
        "io.spray"            %%  "spray-can"        % sprayV,
        "io.spray"            %%  "spray-client"     % sprayV,
        "io.spray"            %%  "spray-json"       % "1.3.2",
        "io.spray"            %%  "spray-routing"    % sprayV,
        "io.spray"            %%  "spray-testkit"    % sprayV  % "test",
        "net.jcazevedo"       %%  "moultingyaml"     % "0.1",
        "net.virtual-void"    %%  "json-lenses"      % "0.6.0",
        "org.apache.commons"  %   "commons-io"       % "1.3.2",
        "org.specs2"          %%  "specs2-core"      % specs2V % "test",
        "org.specs2"          %%  "specs2-mock"      % specs2V % "test",
        "org.xerial"          %   "sqlite-jdbc"      % "3.8.10.1"
      )
    }
  )

Revolver.settings
