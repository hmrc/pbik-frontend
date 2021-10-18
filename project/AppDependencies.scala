import sbt._

object AppDependencies {

  val silencerVersion = "1.7.6"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"     %% "play-partials"                % "8.2.0-play-28",
    "uk.gov.hmrc"     %% "url-builder"                  % "3.5.0-play-28",
    "uk.gov.hmrc"     %% "tax-year"                     % "1.6.0",
    "uk.gov.hmrc"     %% "bootstrap-frontend-play-28"   % "5.16.0",
    "uk.gov.hmrc"     %% "play-frontend-hmrc"           % "1.21.0-play-28",
    "uk.gov.hmrc"     %% "http-caching-client"          % "9.5.0-play-28",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )

  val test: Seq[ModuleID] = Seq(
    "org.mockito"               % "mockito-core"          % "3.12.4",
    "org.specs2"                %% "specs2-core"          % "4.13.0",
    "org.jsoup"                 % "jsoup"                 % "1.14.3",
    "org.scalatestplus.play"    %% "scalatestplus-play"   % "5.1.0",
    "org.scalatestplus"         %% "mockito-3-4"          % "3.2.10.0",
    "com.vladsch.flexmark"      % "flexmark-all"          % "0.35.10",
    "org.pegdown"               % "pegdown"               % "1.6.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
