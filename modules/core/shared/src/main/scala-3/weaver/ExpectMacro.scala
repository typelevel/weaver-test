package weaver

import scala.quoted._
import scala.language.experimental.macros
import cats.syntax.all.*

import weaver.internals.Clues

private[weaver] trait ExpectMacro {

  /**
   * Asserts that a boolean value is true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  inline def apply(assertion: Clues ?=> Boolean)(using
      loc: SourceLocation): Expectations =
    ${ ExpectMacro.applyImpl('assertion, 'loc) }

    /**
     * Asserts that a boolean value is true and displays a failure message if
     * not.
     *
     * Use the [[Expectations.Helpers.clue]] function to investigate any
     * failures.
     */
  inline def apply(
      assertion: Clues ?=> Boolean,
      message: => String)(using loc: SourceLocation): Expectations =
    ${ ExpectMacro.applyMessageImpl('assertion, 'message, 'loc) }

  /**
   * Asserts that boolean values are all true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  inline def all(inline assertions: (Clues ?=> Boolean)*)(using
      loc: SourceLocation): Expectations =
    ${ ExpectMacro.allImpl('assertions, 'loc) }
}
private[weaver] object ExpectMacro {

  /**
   * Constructs [[Expectations]] from an assertion.
   *
   * A macro is needed to extract the source location of the `expect` call.
   *
   * This macro constructs a local collection of [[Clues]]. Calls to
   * [[ClueHelpers.clue]] add to this collection.
   *
   * After the assertion is evaluated, the [[Clues]] collection is used to
   * contruct [[Expectations]].
   */
  def applyImpl[T: Type](
      assertion: Expr[Clues ?=> Boolean],
      loc: Expr[SourceLocation])(using q: Quotes): Expr[Expectations] = {
    import q.reflect.*
    // The compiler doesn't return the correct position information
    // for `assertion.asTerm.pos`. Use the position of the entire
    // expect statement instead.
    val sourceCode = Expr(Position.ofMacroExpansion.sourceCode)
    '{
      val clues  = new Clues
      val result = ${ assertion }(using clues)
      Clues.toExpectations($loc,
                           sourceCode = $sourceCode,
                           message = None,
                           clues,
                           result)
    }
  }

  /**
   * Constructs [[Expectations]] from an assertion and message.
   *
   * If the assertion evaluates to false, the message is displayed as part of
   * the failed expectation.
   */
  def applyMessageImpl[T: Type](
      assertion: Expr[Clues ?=> Boolean],
      message: => Expr[String],
      loc: Expr[SourceLocation])(using q: Quotes): Expr[Expectations] = {
    import q.reflect.*
    val sourceCode = Expr(Position.ofMacroExpansion.sourceCode)
    '{
      val clues  = new Clues
      val result = ${ assertion }(using clues)
      Clues.toExpectations($loc,
                           sourceCode = $sourceCode,
                           message = Some($message),
                           clues,
                           result)
    }
  }

  /**
   * Constructs [[Expectations]] from several assertions.
   *
   * If any assertion evaluates to false, all generated clues are displayed as
   * part of the failed expectation.
   */
  def allImpl[T: Type](
      assertions: Expr[Seq[(Clues ?=> Boolean)]],
      loc: Expr[SourceLocation])(using q: Quotes): Expr[Expectations] = {
    import q.reflect.*
    val sourceCodes: Expr[List[Option[String]]] = Expr(assertions match {
      case Varargs(exprs) => exprs.toList.map(_.asTerm.pos.sourceCode)
      case _              => Nil
    })
    '{
      val expectations =
        ${ assertions }.zipWithIndex.map { case (assertion, index) =>
          val clues      = new Clues
          val result     = assertion(using clues)
          val sourceCode = ${ sourceCodes }.get(index).flatten
          Clues.toExpectations($loc,
                               sourceCode = sourceCode,
                               message = None,
                               clues,
                               result)
        }
      expectations.toList.combineAll
    }
  }

}
