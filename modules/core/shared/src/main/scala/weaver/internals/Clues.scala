package weaver.internals
import scala.annotation.implicitNotFound
import cats.data.{ NonEmptyList, Validated }

import weaver.Expectations
import weaver.AssertionException
import weaver.SourceLocation
import cats.data.Chain

// For Scala 3, the Clues collection is provided implicitly using a context function.
// If users attempt to call the `clue` function outside of the `expect` context, they will get this implicitNotFound error.
@implicitNotFound(
  "The `clue` function can only be called within `expect` or `assert`.")
/**
 * A collection of all the clues defined within a call to `expect`.
 *
 * Each call to `expect` has its own unique clue collection. When a [[Clue]] is
 * evaluated, it is added to the collection with [[addClue]].
 */
final class Clues {
  private var clues: Chain[Clue[?]] = Chain.empty

  /**
   * Adds a clue to the collection.
   *
   * This function is called as part of the expansion of the `expect` macro. It
   * should not be called explicitly.
   */
  def addClue[A](clue: Clue[A]): A = {
    clues = clues :+ clue
    clue.value
  }

  private[Clues] def getClues: List[Clue[?]] = clues.toList
}

object Clues {

  /**
   * Constructs [[Expectations]] from the collection of clues.
   *
   * If the results are successful, the clues are discarded. If any result has
   * failed, the clues are printed as part of the failure message.
   *
   * This function is called as part of the expansion of the `expect` macro. It
   * should not be called explicitly.
   */
  def toExpectations(
      sourceLoc: SourceLocation,
      message: Option[String],
      clues: Clues,
      results: Boolean*): Expectations = {
    val success = results.toList.forall(identity)
    if (success) {
      Expectations(Validated.valid(()))
    } else {
      val header   = "assertion failed" + message.fold("")(msg => s": $msg")
      val clueList = clues.getClues
      val cluesMessage = if (clueList.nonEmpty) {
        val lines = clueList.map(clue => s"  ${clue.prettyPrint}")
        lines.mkString("Clues {\n", "\n", "\n}")
      } else ""
      val fullMessage = header + "\n\n" + cluesMessage

      val exception =
        new AssertionException(fullMessage, NonEmptyList.of(sourceLoc))
      Expectations(Validated.invalidNel(exception))
    }
  }
}
