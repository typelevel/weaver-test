package weaver

import cats.Eq
import cats.Show
import com.eed3si9n.expecty._

trait Comparison[A] {
  def diff(expected: A, found: A): Option[String]
}

object Comparison {
  implicit def fromEqAndShow[A](
      implicit eqv: Eq[A],
      showA: Show[A] = Show.fromToString[A]
  ): Comparison[A] = {
    new Comparison[A] {
      def diff(expected: A, found: A): Option[String] = {
        if (eqv.eqv(found, expected)) {
          None
        } else {
          val expectedLines = showA.show(expected).linesIterator.toSeq
          val foundLines    = showA.show(found).linesIterator.toSeq
          val diff = DiffUtil
            .mkColoredLineDiff(expectedLines, foundLines)
            .linesIterator
            .toSeq
            .map(str => Console.RESET.toString + str)
            .mkString("\n")
          Some(diff)
        }
      }
    }
  }
}
