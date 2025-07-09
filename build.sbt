import Dependencies._
import de.heikoseeberger.sbtheader.License

ThisBuild / scalaVersion     := "2.13.11"
ThisBuild / version          := "0.6-SNAPSHOT"
ThisBuild / organization     := "com.ideal.linked"

lazy val root = (project in file("."))
  .settings(
    name := "toposoid-common",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.15",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.31",
    libraryDependencies += "com.typesafe.play" %% "play" % "2.8.18",
    libraryDependencies += "com.ideal.linked" %% "scala-common" % "0.6-SNAPSHOT",
    libraryDependencies += "com.ideal.linked" %% "toposoid-knowledgebase-model" % "0.6-SNAPSHOT",
    libraryDependencies += "com.ideal.linked" %% "toposoid-deduction-protocol-model" % "0.6-SNAPSHOT",
    libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "2.0.2",
    libraryDependencies += "io.jvm.uuid" %% "scala-uuid" % "0.3.1",
    libraryDependencies +=   "com.softwaremill.sttp.client4" %% "core" % "4.0.9",
    libraryDependencies += scalaTest % Test
  )
  .enablePlugins(AutomateHeaderPlugin)

organizationName := "Linked Ideal LLC.[https://linked-ideal.com/]"
startYear := Some(2021)
licenses += ("AGPL-3.0-or-later", new URL("http://www.gnu.org/licenses/agpl-3.0.en.html"))
headerLicense := Some(License.AGPLv3OrLater("2025", organizationName.value))

