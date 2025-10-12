import Dependencies._
import de.heikoseeberger.sbtheader.License

ThisBuild / scalaVersion     := "3.3.6"
ThisBuild / version          := "0.7-SNAPSHOT"
ThisBuild / organization     := "com.ideal.linked"

val AkkaVersion = "2.10.9"
val AkkaHttpVersion = "10.5.2"
val AkkaToken = sys.env.get("TOPOSOID_AKKA_TOKEN")
lazy val root = (project in file("."))
  .settings(
    name := "toposoid-common",
    resolvers in ThisBuild += "akka-secure-mvn" at "https://repo.akka.io/" + AkkaToken + "/secure",
    resolvers in ThisBuild += Resolver.url("akka-secure-ivy", url("https://repo.akka.io/" + AkkaToken  + "/secure"))(Resolver.ivyStylePatterns),
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.7.2",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    libraryDependencies += "org.playframework" %% "play" % "3.0.9",
    libraryDependencies += "com.ideal.linked" %% "scala-common" % "0.7-SNAPSHOT",
    libraryDependencies += "com.ideal.linked" %% "toposoid-knowledgebase-model" % "0.7-SNAPSHOT",
    libraryDependencies += "com.ideal.linked" %% "toposoid-deduction-protocol-model" % "0.7-SNAPSHOT",
    libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "9.0.2",
    libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.12",
    libraryDependencies += scalaTest % Test
  )
  .enablePlugins(AutomateHeaderPlugin)

organizationName := "Linked Ideal LLC.[https://linked-ideal.com/]"
startYear := Some(2021)
licenses += ("AGPL-3.0-or-later", new URL("http://www.gnu.org/licenses/agpl-3.0.en.html"))
headerLicense := Some(License.AGPLv3OrLater("2025", organizationName.value))

