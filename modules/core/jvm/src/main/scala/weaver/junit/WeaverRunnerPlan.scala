package weaver
package junit

/**
 * A list of all tests within a suite.
 *
 * When a suite is executed by the JUnit-based IDE, a progress indicator is
 * displayed for each test name.
 */
private[weaver] case class WeaverRunnerPlan(
    ignoredTests: List[String],
    filteredTests: List[String])
private[weaver] object WeaverRunnerPlan {
  def apply(result: TagAnalysisResult[_]): WeaverRunnerPlan = result match {
    case TagAnalysisResult.Outcomes(ignored, outcomes) =>
      WeaverRunnerPlan(ignored.toList, outcomes.map(_.name).toList)
    case TagAnalysisResult.FilteredTests(ignored, tests) =>
      WeaverRunnerPlan(ignored.toList, tests.map(_._1).toList)
  }
}
