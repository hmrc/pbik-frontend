import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "8.4.0"
  private val hmrcMongoPlayVersion = "1.7.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "tax-year"                   % "4.0.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoPlayVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "8.5.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "org.mockito"                  %% "mockito-scala-scalatest" % "1.17.30",
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"  % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-30" % hmrcMongoPlayVersion,
    "org.jsoup"                     % "jsoup"                   % "1.17.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.16.1"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
