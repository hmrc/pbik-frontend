import sbt._


object FrontendBuild extends Build with MicroService {

  val appName = "pbik-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  private val frontendBootstrapVersion = "12.4.0"
  private val playPartials = "6.5.0"
  private val urlBuilderVersion = "3.1.0"
  private val hmrcTestVersion = "3.6.0-play-25"
  private val mockitoVersion = "1.10.19"
  private val specs2Version = "2.3.13"
  private val jSoupVersion = "1.11.3"
  private val scalatestPlusPlayVersion = "2.0.1"
  private val taxYearVersion = "0.5.0"

  val compile = Seq(

    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartials,
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "tax-year" % taxYearVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = Seq()
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.mockito" % "mockito-all" % mockitoVersion,
        "org.specs2" % "specs2_2.10" % specs2Version,
        "org.jsoup" % "jsoup" % jSoupVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
