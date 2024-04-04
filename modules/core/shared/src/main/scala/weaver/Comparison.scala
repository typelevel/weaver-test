package weaver

import cats.Eq
import cats.Show
import com.eed3si9n.expecty._
import scala.annotation.implicitNotFound

@implicitNotFound("Could not find an implicit Comparison[${A}]. Does ${A} have an associated cats.Eq[${A}] instance?")
trait Comparison[A] {
  def diff(expected: A, found: A): Comparison.Result
}

object Comparison {
  sealed trait Result
  object Result {
    case object Success              extends Result
    case class Failure(diff: String) extends Result
  }

  /**
   * Create a `Comparison` instance from an `Eq` implementation. Uses the
   * default `Show.fromToString` when no implicit `Show` instance is found.
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
          val diff = DiffUtil
            .mkColoredLineDiff(expectedLines, foundLines)
            .linesIterator
            .toSeq
            .map(str => Console.RESET.toString + str)
            .mkString("\n")
          Result.Failure(diff)
        }
      }
    }
  }

  /**
   * Create a `Comparison` instance from an `diff` implementation.
   */
  def instance[A](f: (A, A) => Option[String]): Comparison[A] =
    new Comparison[A] {
      def diff(expected: A, found: A): Result = f(expected, found) match {
        case None       => Result.Success
        case Some(diff) => Result.Failure(diff)
      }
    }

  /**
   * Create a `Comparison` instance from an `diff` implementation.
   */
  def instance[A](f: PartialFunction[(A, A), String]): Comparison[A] =
    instance((expected, found) => f.lift((expected, found)))
}
