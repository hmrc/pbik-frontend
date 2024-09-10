import sbt.Setting
import scoverage.ScoverageKeys.*

object CodeCoverageSettings {

  val settings: Seq[Setting[?]] = Seq(
    coverageExcludedFiles := ".*Routes.*",
    //TODO raise it back to 90+%
    coverageMinimumStmtTotal := 75,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )

}
