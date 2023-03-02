import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "7.13.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc" %% "tax-year"                   % "3.0.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc"         % "6.4.0-play-28",
    "uk.gov.hmrc" %% "http-caching-client"        % "10.0.0-play-28"
  )

  private val test: Seq[ModuleID] = Seq(
    "org.mockito"                  %% "mockito-scala-scalatest" % "1.17.12",
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28"  % bootstrapPlayVersion,
    "org.jsoup"                     % "jsoup"                   % "1.15.4",
    "org.scalatest"                %% "scalatest"               % "3.2.15",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.14.2",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.62.2"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
