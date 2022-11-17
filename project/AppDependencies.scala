import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc" %% "url-builder"                % "3.7.0-play-28",
    "uk.gov.hmrc" %% "tax-year"                   % "3.0.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "7.11.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc"         % "3.33.0-play-28",
    "uk.gov.hmrc" %% "http-caching-client"        % "10.0.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "org.mockito"                  %% "mockito-scala-scalatest" % "1.17.12",
    "org.specs2"                   %% "specs2-core"             % "4.19.0",
    "org.jsoup"                     % "jsoup"                   % "1.15.3",
    "org.scalatestplus.play"       %% "scalatestplus-play"      % "5.1.0",
    "org.scalatest"                %% "scalatest"               % "3.2.14",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.14.0",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.62.2"
  ).map(_ % "test")

  val all: Seq[ModuleID]  = compile ++ test
}
