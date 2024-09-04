package weaver.internals
import cats.Show
import scala.quoted._
import scala.language.experimental.macros

private[weaver] object ClueMacro {

  /**
   * Constructs a clue for a given value and adds it to a collection of
   * [[Clues]], then returns the value.
   */
  def clueImpl[T: Type](
      value: Expr[T],
      catsShow: Expr[Show[T]],
      clues: Expr[Clues])(using Quotes): Expr[T] = {
    import quotes.reflect._
    val source    = value.asTerm.pos.sourceCode.getOrElse("")
    val valueType = TypeTree.of[T].show(using Printer.TreeShortCode)
    '{
      val clue =
        new Clue(${ Expr(source) }, $value, ${ Expr(valueType) }, $catsShow)
      $clues.addClue(clue)
    }
  }
}
