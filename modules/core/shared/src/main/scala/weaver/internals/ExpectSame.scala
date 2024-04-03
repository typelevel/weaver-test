package weaver
package internals

import cats.Show
import cats.data.{ NonEmptyList, Validated }
import cats.kernel.Eq

private[weaver] trait ExpectSame {

  def eql[A](
      expected: A,
      found: A)(
      implicit eqA: Eq[A],
      showA: Show[A] = Show.fromToString[A],
      loc: SourceLocation): Expectations = {
    val comparison = Comparison.fromEqAndShow(eqA, showA)
    diff(expected, found)(comparison, loc)
  }

  /**
   * Same as eql but defaults to universal equality.
   */
  def same[A](
      expected: A,
      found: A)(
      implicit eqA: Eq[A] = Eq.fromUniversalEquals[A],
      showA: Show[A] = Show.fromToString[A],
      loc: SourceLocation): Expectations = eql(expected, found)(eqA, showA, loc)

  def diff[A](
      expected: A,
      found: A)(
      implicit comparisonA: Comparison[A],
      loc: SourceLocation): Expectations = {
    comparisonA.diff(expected, found) match {
      case None => Expectations(Validated.validNel(()))
      case Some(diff) =>
        val header     = "Values not equal:"
        val sourceLocs = NonEmptyList.of(loc)
        Expectations(
          Validated.invalidNel[AssertionException, Unit](
            new AssertionException(header + "\n\n" + diff, sourceLocs)))
    }
  }
}
