import sbt._

object AppDependencies {

  val silencerVersion = "1.7.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-27",
    "uk.gov.hmrc" %% "url-builder" % "3.5.0-play-27",
    "uk.gov.hmrc" %% "tax-year" % "1.3.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.3.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "0.68.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.4.0-play-27",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
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
