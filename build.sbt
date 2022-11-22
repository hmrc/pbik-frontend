import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "pbik-frontend"
scalaVersion := "2.12.16"

lazy val scoverageSettings: Seq[Def.Setting[_]] =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimumStmtTotal := 88,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(SbtDistributablesPlugin, play.sbt.PlayScala, SbtWeb)
  .settings(
    scoverageSettings,
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    majorVersion := 7,
    PlayKeys.playDefaultPort := 9233,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    Assets / unmanagedResourceDirectories += baseDirectory.value / "app" / "assets",
    Assets / excludeFilter := "js*" || "sass*",
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    )
  )

scalacOptions ++= Seq(
  "-feature",
  "-Wconf:src=routes/.*:s",
  "-Wconf:cat=unused-imports&src=html/.*:s"
)

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle test:scalastyle")
