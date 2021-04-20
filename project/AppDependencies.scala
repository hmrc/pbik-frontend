import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-27",
    "uk.gov.hmrc" %% "url-builder" % "3.5.0-play-27",
    "uk.gov.hmrc" %% "tax-year" % "1.3.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "4.2.0",
    "uk.gov.hmrc" %% "play-ui" % "8.21.0-play-27",
    "uk.gov.hmrc" %% "govuk-template" % "5.65.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.4.0-play-27"
  )

  val test: Seq[ModuleID] = Seq(
    "org.mockito" % "mockito-core" % "3.9.0",
    "org.specs2" %% "specs2-core" % "4.10.6",
    "org.jsoup" % "jsoup" % "1.13.1",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3",
    "org.pegdown" % "pegdown" % "1.6.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
