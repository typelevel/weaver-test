package weaver.internals
import cats.Show
import scala.quoted._
import scala.language.experimental.macros

private[weaver] trait ClueHelpers {

  /**
   * Used to investigate failures in `expect` statements.
   *
   * Surround a value with a call to `clue` to display it on failure.
   */
  inline def clue[A](value: A)(
      using catsShow: Show[A] = Show.fromToString[A],
      clues: Clues): A = ${ ClueMacro.clueImpl('value, 'catsShow, 'clues) }
}
