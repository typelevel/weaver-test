package weaver
package internals

import cats.Show
import cats.data.{ NonEmptyList, Validated }
import cats.kernel.Eq

private[weaver] trait ExpectSame {

  def eql[A](
      expected: A,
      found: A)(
      implicit comparisonA: Comparison[A],
      loc: SourceLocation): Expectations = {
    comparisonA.diff(expected, found) match {
      case Comparison.Result.Success => Expectations(Validated.validNel(()))
      case Comparison.Result.Failure(report) =>
        val header     = "Values not equal:"
        val sourceLocs = NonEmptyList.of(loc)
        Expectations(
          Validated.invalidNel[AssertionException, Unit](
            new AssertionException(header + "\n\n" + report, sourceLocs)))
    }
  }

  /**
   * Same as eql but defaults to universal equality.
   */
  def same[A](
      expected: A,
      found: A)(
      implicit comparisonA: Comparison[A] =
        Comparison.fromEq[A](Eq.fromUniversalEquals, Show.fromToString),
      loc: SourceLocation): Expectations =
    eql(expected, found)(comparisonA, loc)
}

private[weaver] object ExpectSame extends ExpectSame
