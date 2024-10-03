import sbt.Setting
import scoverage.ScoverageKeys.*

object CodeCoverageSettings {

  val settings: Seq[Setting[?]] = Seq(
    coverageExcludedFiles := ".*Routes.*",
    //TODO raise it back to 95+%
    coverageMinimumStmtTotal := 93,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )

}
