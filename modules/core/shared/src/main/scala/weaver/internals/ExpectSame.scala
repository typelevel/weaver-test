package weaver
package internals

import cats.data.{ NonEmptyList, Validated }
import cats.kernel.Eq

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
        val header     = s"Values not equal:\n\nin expect.$functionName"
        val sourceLocs = NonEmptyList.of(loc)
        Expectations(
          Validated.invalidNel[ExpectationFailed, Unit](
            new ExpectationFailed(
              header + report,
              sourceLocs)))
    }
  }

}
