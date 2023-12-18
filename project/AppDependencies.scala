import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "7.23.0"
  private val hmrcMongoPlayVersion = "1.3.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc"       %% "tax-year"                   % "3.3.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % hmrcMongoPlayVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "7.29.0-play-28"
  )

  private val test: Seq[ModuleID] = Seq(
    "org.mockito"                  %% "mockito-scala-scalatest" % "1.17.30",
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28"  % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-28" % hmrcMongoPlayVersion,
    "org.jsoup"                     % "jsoup"                   % "1.17.1",
    "org.scalatest"                %% "scalatest"               % "3.2.17",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.16.0",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
