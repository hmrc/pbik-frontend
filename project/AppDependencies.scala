import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-partials" % "6.11.0-play-26",
    "uk.gov.hmrc" %% "url-builder" % "3.1.0",
    "uk.gov.hmrc" %% "tax-year" % "1.1.0",
    "uk.gov.hmrc" %% "auth-client" % "2.35.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.8.0",
    "uk.gov.hmrc" %% "play-ui" % "8.12.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.55.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.1.0-play-26"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26",
    "org.mockito" % "mockito-all" % "1.10.19",
    "org.specs2" %% "specs2-core" % "4.9.4",
    "org.jsoup" % "jsoup" % "1.13.1",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
