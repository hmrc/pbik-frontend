import sbt.Opts.compile
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.7.0",
    "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-25",
    "uk.gov.hmrc" %% "url-builder" % "3.1.0",
    "uk.gov.hmrc" %% "tax-year" % "0.5.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.8.0-play-25",
    "org.mockito" % "mockito-all" % "1.10.19",
    "org.specs2" % "specs2_2.10" % "2.3.13",
    "org.jsoup" % "jsoup" % "1.11.3",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
