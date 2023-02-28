import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "pbik-frontend"

lazy val scoverageSettings: Seq[Def.Setting[_]] =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimumStmtTotal := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(SbtDistributablesPlugin, play.sbt.PlayScala, SbtWeb)
  .settings(
    scoverageSettings,
    scalaSettings,
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    scalaVersion := "2.13.10",
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

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
