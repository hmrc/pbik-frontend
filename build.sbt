val appName = "pbik-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(SbtDistributablesPlugin, play.sbt.PlayScala, SbtWeb)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  // To resolve dependency clash between flexmark v0.64.4+ and play-language to run accessibility tests, remove when versions align
  .settings(dependencyOverrides += "com.ibm.icu" % "icu4j" % "69.1")
  .settings(
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    majorVersion := 7,
    PlayKeys.playDefaultPort := 9233,
    libraryDependencies ++= AppDependencies(),
    routesGenerator := InjectedRoutesGenerator,
    Assets / unmanagedResourceDirectories += baseDirectory.value / "app" / "assets",
    Assets / excludeFilter := "js*" || "sass*",
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    )
  )
  .settings(scalaVersion := "2.13.12")
  .settings(
    coverageExcludedPackages := "<empty>;Reverse.*;.*(AuthService|BuildInfo|Routes).*",
    coverageMinimumStmtTotal := 91,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )

scalacOptions ++= Seq(
  "-feature",
  "-Wconf:src=routes/.*:s",
  "-Wconf:cat=unused-imports&src=views/.*:s"
)

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt A11y/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle A11y/scalastyle")
