import Dependencies._
import de.heikoseeberger.sbtheader.License

ThisBuild / scalaVersion     := "3.3.6"
ThisBuild / version          := "0.7-SNAPSHOT"
ThisBuild / organization     := "com.ideal.linked"

//val AkkaVersion = "2.10.11"
//val AkkaHttpVersion = "10.7.3"
//val AkkaToken = sys.env.get("TOPOSOID_AKKA_TOKEN").get
val PekkoVersion = "1.1.5"
val PekkoHttpVersion = "1.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "toposoid-common",
    libraryDependencies += "org.playframework" %% "play" % "3.0.7" exclude("org.slf4j","slf4j-api"),
    libraryDependencies += "com.ideal.linked" %% "scala-common" % "0.7-SNAPSHOT" exclude("org.slf4j","slf4j-api"),
    libraryDependencies += "com.ideal.linked" %% "toposoid-knowledgebase-model" % "0.7-SNAPSHOT" exclude("org.slf4j","slf4j-api"),
    libraryDependencies += "com.ideal.linked" %% "toposoid-deduction-protocol-model" % "0.7-SNAPSHOT" exclude("org.slf4j","slf4j-api"),    
    libraryDependencies += "org.apache.pekko" %% "pekko-connectors-sqs" % "1.2.0" exclude("org.slf4j","slf4j-api"),
    libraryDependencies += "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion exclude("org.slf4j","slf4j-api"),
    libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.12" exclude("org.slf4j","slf4j-api"),
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.36",
    libraryDependencies += scalaTest % Test
  )
  .enablePlugins(AutomateHeaderPlugin)

organizationName := "Linked Ideal LLC.[https://linked-ideal.com/]"
startYear := Some(2021)
licenses += ("AGPL-3.0-or-later", url("http://www.gnu.org/licenses/agpl-3.0.en.html"))
headerLicense := Some(License.AGPLv3OrLater("2025", organizationName.value))

