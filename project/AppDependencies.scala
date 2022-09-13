import sbt._

object AppDependencies {

  val silencerVersion = "1.7.9"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"    %% "play-partials"              % "8.2.0-play-28",
    "uk.gov.hmrc"    %% "url-builder"                % "3.5.0-play-28",
    "uk.gov.hmrc"    %% "tax-year"                   % "3.0.0",
    "uk.gov.hmrc"    %% "bootstrap-frontend-play-28" % "7.2.0",
    "uk.gov.hmrc"    %% "play-frontend-hmrc"         % "3.24.0-play-28",
    "uk.gov.hmrc"    %% "http-caching-client"        % "9.5.0-play-28",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib"               % silencerVersion % Provided cross CrossVersion.full
  )

  val test: Seq[ModuleID] = Seq(
    "org.mockito"                   % "mockito-core"         % "4.7.0",
    "org.specs2"                   %% "specs2-core"          % "4.16.1",
    "org.jsoup"                     % "jsoup"                % "1.15.3",
    "org.scalatestplus.play"       %% "scalatestplus-play"   % "5.1.0",
    "org.scalatest"                %% "scalatest"            % "3.2.13",
    "org.scalatestplus"            %% "mockito-4-6"          % "3.2.13.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4",
    "com.vladsch.flexmark"          % "flexmark-all"         % "0.62.2"
  ).map(_ % "test")

  val all: Seq[ModuleID]  = compile ++ test
}
