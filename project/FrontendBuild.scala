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

  private val frontendBootstrapVersion = "3.0.0"
  private val govukTemplateVersion =  "4.0.0"
  private val httpVerbsVersion = "3.0.0"
  private val playAuthorisedFrontendVersion = "4.0.0"
  private val playConfigVersion = "2.0.1"
  private val playHealthVersion = "1.1.0"
  private val playJsonLogger = "2.1.1"
  private val playPartials = "4.0.0"
  private val playUIVersion = "4.4.0"
  private val urlBuilderVersion = "0.8.0"

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

    "com.codahale.metrics" % "metrics-graphite" % "3.0.2",
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % "2.2.0" % scope,
        "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % "test",
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()

}
