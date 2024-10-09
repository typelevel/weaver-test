package weaver
package internals

import cats.Show
import cats.kernel.Eq

private[weaver] trait AssertSame {

  def eql[A](
      expected: A,
      found: A)(
      implicit comparisonA: Comparison[A],
      loc: SourceLocation): Unit = {
    val expectations = ExpectSame.eql(expected, found)(comparisonA, loc)
    Assert.throwWhenFailed(expectations)
  }

  /**
   * Same as eql but defaults to universal equality.
   */
  def same[A](
      expected: A,
      found: A)(
      implicit comparisonA: Comparison[A] =
        Comparison.fromEq[A](Eq.fromUniversalEquals, Show.fromToString),
      loc: SourceLocation): Unit = eql(expected, found)(comparisonA, loc)
}
