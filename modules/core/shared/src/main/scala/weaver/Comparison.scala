package weaver

import cats.Eq
import cats.Show
import com.eed3si9n.expecty._
import scala.annotation.implicitNotFound

@implicitNotFound("Could not find an implicit Comparison[${A}]. Does ${A} have an associated cats.Eq[${A}] instance?")
/**
 * A type class used to compare two instances of the same type and construct an
 * informative report.
 *
 * If the comparison succeeds with [[Result.Success]] then no report is printed.
 * If the comparison fails with [[Result.Failure]], then the report is printed
 * with the test failure.
 *
 * The report is generally a diff of the `expected` and `found` values. It may
 * use ANSI escape codes to add color.
 */
trait Comparison[A] {
  def diff(expected: A, found: A): Comparison.Result
}

object Comparison {
  sealed trait Result
  object Result {
    case object Success                extends Result
    case class Failure(report: String) extends Result
  }

  /**
   * Create a [[Comparison]] instance from an [[cats.kernel.Eq]] implementation.
   * Uses the [[cats.data.Show]] instance or [[cats.data.Show.fromToString]] to
   * construct a string diff of the `expected` and `found` values on failure.
   */
  implicit def fromEq[A](
      implicit eqv: Eq[A],
      showA: Show[A] = Show.fromToString[A]
  ): Comparison[A] = {
    new Comparison[A] {
      def diff(expected: A, found: A): Result = {
        if (eqv.eqv(found, expected)) {
          Result.Success
        } else {
          val expectedLines = showA.show(expected).linesIterator.toSeq
          val foundLines    = showA.show(found).linesIterator.toSeq
          val report = DiffUtil
            .mkColoredLineDiff(expectedLines, foundLines)
            .linesIterator
            .toSeq
            .map(str => Console.RESET.toString + str)
            .mkString("\n")
          Result.Failure(report)
        }
      }
    }
  }

  /**
   * Create a [[Comparison]] instance from a `diff` implementation.
   */
  def instance[A](f: (A, A) => Option[String]): Comparison[A] =
    new Comparison[A] {
      def diff(expected: A, found: A): Result = f(expected, found) match {
        case None         => Result.Success
        case Some(report) => Result.Failure(report)
      }
    }

  /**
   * Create a [[Comparison]] instance from a `diff` implementation.
   */
  def instance[A](f: PartialFunction[(A, A), String]): Comparison[A] =
    instance((expected, found) => f.lift((expected, found)))
}
