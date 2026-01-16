import sbt.Setting
import scoverage.ScoverageKeys.*

object CodeCoverageSettings {

  private val excludedPackages = Seq(
    "<empty>",
    "Reverse.*",
    ".*Routes.*",
    ".*\\.Reverse.*",
    "views\\.html\\.components.*",
    "views\\.html\\.localhelpers.*"
  ).mkString(";")

  private val settings: Seq[Setting[?]] = Seq(
    coverageExcludedPackages := excludedPackages,
    coverageMinimumStmtTotal := 95,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )

  def apply(): Seq[Setting[?]] = settings

}
