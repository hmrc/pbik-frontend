import sbt.Opts.compile
import sbt._

object AppDependencies {

  private val frontendBootstrapVersion = "12.7.0"
  private val playPartials = "6.9.0-play-25"
  private val urlBuilderVersion = "3.1.0"
  private val hmrcTestVersion = "3.8.0-play-25"
  private val mockitoVersion = "1.10.19"
  private val specs2Version = "2.3.13"
  private val jSoupVersion = "1.11.3"
  private val scalatestPlusPlayVersion = "2.0.1"
  private val taxYearVersion = "0.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartials,
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "tax-year" % taxYearVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq()
  }

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion,
    "org.mockito" % "mockito-all" % mockitoVersion,
    "org.specs2" % "specs2_2.10" % specs2Version,
    "org.jsoup" % "jsoup" % jSoupVersion,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
