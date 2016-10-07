import play.PlayImport.PlayKeys._
import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.SbtWeb
import scoverage.ScoverageSbtPlugin

import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning


object FrontendBuild extends Build with MicroService {

  val appName = "pbik-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

  override lazy val plugins : Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin,
    play.PlayScala, SbtWeb
  )

  override lazy val playSettings : Seq[Setting[_]] = Seq(
    routesImport ++= Seq("uk.gov.hmrc.domain._"),
    // Turn off play's internal less compiler
    lessEntryPoints := Nil,
    // Turn off play's internal javascript compiler
    javascriptEntryPoints := Nil,
    // Add the views to the dist
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    // Dont include the source assets in the dist package (public folder)
    excludeFilter in Assets := "js*" || "sass*"
  ) ++ JavaScriptBuild.javaScriptUiSettings

}

private object AppDependencies {
  import play.core.PlayVersion

  private val frontendBootstrapVersion = "6.7.0"
  private val govukTemplateVersion =  "4.0.0"
  private val httpVerbsVersion = "5.0.0"
  private val playAuthorisedFrontendVersion = "5.7.0"
  private val playConfigVersion = "2.1.0"
  private val playHealthVersion = "1.1.0"
  private val playJsonLogger = "2.1.1"
  private val playPartials = "4.6.0"
  private val playUIVersion = "4.16.0"
  private val urlBuilderVersion = "1.1.0"
  private val scalaTestPlusVersion = "1.2.0"
  private val scalaTestVersion = "2.2.6"
  private val hmrcTestVersion = "1.8.0"
  private val mockitoVersion = "1.10.19"
  private val pegDownVersion = "1.6.0"
  private val jSoupVersion = "1.9.2"
  private val metricsGraphiteVersion = "3.0.2"
  private val metricsPlayVersion = "0.2.1"

  val compile = Seq(

    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion, // includes the global object and error handling, as well as the FrontendController classes
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "http-verbs" % httpVerbsVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthorisedFrontendVersion, // use when your frontend requires authentication
    "uk.gov.hmrc" %% "play-config" % playConfigVersion, // includes helper classes for retrieving Play configuration
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-json-logger" % playJsonLogger,
    "uk.gov.hmrc" %% "play-partials" % playPartials, // includes code for retrieving partials, e.g. the Help with this page form
    "uk.gov.hmrc" %% "play-ui" % playUIVersion,
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,
    "com.kenshoo" %% "metrics-play" % metricsPlayVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = Seq()
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatestplus" %% "play" % scalaTestPlusVersion % scope,
        "org.pegdown" % "pegdown" % pegDownVersion % scope,
        "org.mockito" % "mockito-all" % mockitoVersion % "test",
        "org.jsoup" % "jsoup" % jSoupVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()

}
