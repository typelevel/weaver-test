package weaver
package internals

import cats.data.{ NonEmptyList, Validated }
import cats.kernel.Eq
import munit.diff.console.AnsiColors

private[weaver] trait ExpectSame {

  def eql[A](
      expected: A,
      found: A)(
      implicit comparisonA: Comparison[A],
      loc: SourceLocation): Expectations = {
    eql(expected, found, "eql")(comparisonA, loc)
  }

  /**
   * Same as eql but defaults to universal equality.
   */
  def same[A](
      expected: A,
      found: A)(
      implicit comparisonA: Comparison[A] =
        Comparison.fromEq[A](Eq.fromUniversalEquals, MultiLineShow.show),
      loc: SourceLocation): Expectations =
    eql(expected, found, "same")(comparisonA, loc)

  private def eql[A](
      expected: A,
      found: A,
      functionName: String)(
      implicit comparisonA: Comparison[A],
      loc: SourceLocation): Expectations = {
    comparisonA.diff(expected, found) match {
      case Comparison.Result.Success => Expectations(Validated.validNel(()))
      case Comparison.Result.Failure(report) =>
        // Use the same colours as munit-diff's output.
        val expectedHeader = AnsiColors.c("- expected", AnsiColors.LightRed)
        val obtainedHeader = AnsiColors.c("+ found", AnsiColors.LightGreen)
        val header =
          s"Values not equal:\n\nin expect.$functionName($expectedHeader, $obtainedHeader)"
        val sourceLocs = NonEmptyList.of(loc)
        Expectations(
          Validated.invalidNel[ExpectationFailed, Unit](
            new ExpectationFailed(
              header + "\n" + report,
              sourceLocs)))
    }
  }

}
