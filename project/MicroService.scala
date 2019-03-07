import com.typesafe.sbt.web.SbtWeb
import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import com.typesafe.sbt.web.SbtWeb.autoImport._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

trait MicroService {

  import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings, targetJvm}
  import uk.gov.hmrc._


  val appName: String

  val appDependencies : Seq[ModuleID]
  lazy val plugins : Seq[Plugins] = Seq(SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, play.sbt.PlayScala, SbtWeb, SbtArtifactory)

  lazy val scoverageSettings = {
    import ScoverageSbtPlugin._

    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
        ".*(AuthService|BuildInfo|Routes).*",
      ScoverageKeys.coverageMinimum := 75,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )

  }

  val wartRemovedExcludedClasses = Seq(
    "app.Routes", "prod.Routes", "app.routes", "prod.routes", "uk.gov.hmrc.BuildInfo",
    "controllers.routes", "controllers.javascript", "controllers.ref"
  )

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(plugins : _*)
    .settings(scoverageSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      routesGenerator := StaticRoutesGenerator,
      unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
      excludeFilter in Assets := "js*" || "sass*",
      JavaScriptBuild.javaScriptUiSettings
    )
    .settings(
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.typesafeRepo("releases"),
        Resolver.jcenterRepo
      )
    )
    .settings(majorVersion := 7)

}
