import Dependencies._

ThisBuild / scalaVersion     := "2.13.11"
ThisBuild / version          := "0.5"
ThisBuild / organization     := "com.ideal.linked"

lazy val root = (project in file("."))
  .settings(
    name := "toposoid-common",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.15",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.31",
    libraryDependencies += "com.typesafe.play" %% "play" % "2.8.18",
    libraryDependencies += "com.ideal.linked" %% "scala-common" % "0.5",
    libraryDependencies += scalaTest % Test
  )
  .enablePlugins(AutomateHeaderPlugin)

organizationName := "Linked Ideal LLC.[https://linked-ideal.com/]"
startYear := Some(2021)
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

