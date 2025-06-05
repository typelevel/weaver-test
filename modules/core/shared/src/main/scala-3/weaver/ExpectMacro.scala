package weaver

import scala.quoted._
import scala.language.experimental.macros
import weaver.internals.Clues

private[weaver] trait ExpectMacro {

  /**
   * Asserts that a boolean value is true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  inline def apply(assertion: Clues ?=> Boolean): Expectations =
    ${ ExpectMacro.applyImpl('assertion) }

    /**
     * Asserts that a boolean value is true and displays a failure message if
     * not.
     *
     * Use the [[Expectations.Helpers.clue]] function to investigate any
     * failures.
     */
  inline def apply(
      assertion: Clues ?=> Boolean,
      message: => String): Expectations =
    ${ ExpectMacro.applyMessageImpl('assertion, 'message) }

  /**
   * Asserts that boolean values are all true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  inline def all(assertions: (Clues ?=> Boolean)*): Expectations =
    ${ ExpectMacro.allImpl('assertions) }
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
  def applyImpl[T: Type](assertion: Expr[Clues ?=> Boolean])(using
      q: Quotes): Expr[Expectations] = {
    val sourceLoc = weaver.macros.fromContextImpl(using q)
    '{
      val clues  = new Clues
      val result = ${ assertion }(using clues)
      Clues.toExpectations($sourceLoc, None, clues, result)
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
      message: => Expr[String])(using q: Quotes): Expr[Expectations] = {
    val sourceLoc = weaver.macros.fromContextImpl(using q)
    '{
      val clues  = new Clues
      val result = ${ assertion }(using clues)
      Clues.toExpectations($sourceLoc, Some($message), clues, result)
    }
  }

  /**
   * Constructs [[Expectations]] from several assertions.
   *
   * If any assertion evaluates to false, all generated clues are displayed as
   * part of the failed expectation.
   */
  def allImpl[T: Type](assertions: Expr[Seq[(Clues ?=> Boolean)]])(using
      q: Quotes): Expr[Expectations] = {
    val sourceLoc = weaver.macros.fromContextImpl(using q)
    '{
      val clues   = new Clues
      val results = ${ assertions }.map(assertion => assertion(using clues))
      Clues.toExpectations($sourceLoc, None, clues, results: _*)
    }
  }

}
