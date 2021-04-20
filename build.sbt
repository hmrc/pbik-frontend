import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile

val appName = "pbik-frontend"
scalaVersion := "2.12.12"

lazy val plugins: Seq[Plugins] =
  Seq(SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, play.sbt.PlayScala, SbtWeb)
lazy val scoverageSettings: Seq[Def.Setting[_]] =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 85,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scoverageSettings,
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    majorVersion := 7,
    scalafmtOnCompile := true,
    PlayKeys.playDefaultPort := 9233,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    excludeFilter in Assets := "js*" || "sass*",
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.play.views.html.helpers._",
      "uk.gov.hmrc.play.views.html.layouts._"
    ),
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/",
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    )
  )
