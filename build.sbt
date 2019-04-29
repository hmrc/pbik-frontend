import sbt.Def
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import ScoverageSbtPlugin._

val appName = "pbik-frontend"

lazy val plugins: Seq[Plugins] = Seq(SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, play.sbt.PlayScala, SbtWeb, SbtArtifactory)
lazy val scoverageSettings: Seq[Def.Setting[_]] =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 75,
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
    PlayKeys.playDefaultPort := 9233,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesGenerator := StaticRoutesGenerator,
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    excludeFilter in Assets := "js*" || "sass*",
    JavaScriptBuild.javaScriptUiSettings,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/",
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    )
  )

    


