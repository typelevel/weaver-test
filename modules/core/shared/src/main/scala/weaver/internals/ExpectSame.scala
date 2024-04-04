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
      case Comparison.Result.Failure(diff) =>
        val header     = "Values not equal:"
        val sourceLocs = NonEmptyList.of(loc)
        Expectations(
          Validated.invalidNel[AssertionException, Unit](
            new AssertionException(header + "\n\n" + diff, sourceLocs)))
    }
  }

  /**
   * Same as eql but defaults to universal equality.
   */
  def same[A](
      expected: A,
      found: A)(
      implicit loc: SourceLocation): Expectations = eql(expected, found)(
    Comparison.fromEq(Eq.fromUniversalEquals, Show.fromToString),
    loc)
}
