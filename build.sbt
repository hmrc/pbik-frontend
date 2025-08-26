ThisBuild / scalaVersion := "3.6.4"
ThisBuild / majorVersion := 9

lazy val microservice = Project("pbik-frontend", file("."))
  .enablePlugins(SbtDistributablesPlugin, play.sbt.PlayScala, SbtWeb)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    PlayKeys.playDefaultPort := 9233,
    libraryDependencies ++= AppDependencies(),
    CodeCoverageSettings(),
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:src=routes/.*:s",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:msg=unused import&src=html/.*:s"
    ),
    routesGenerator := InjectedRoutesGenerator,
    Assets / unmanagedResourceDirectories += baseDirectory.value / "app" / "assets",
    Assets / excludeFilter := "js*" || "sass*",
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "models.v1._",
      "models.v1.IabdType",
      "models.v1.IabdType._",
      "models.v1.exclusion._",
      "models.v1.trace._",
      "models.agent._",
      "models.auth._",
      "models.cache._",
      "models.form._"
    ),
    routesImport ++= Seq(
      "models.bindable.Bindables._",
      "models.v1.IabdType.IabdType"
    )
  )

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
