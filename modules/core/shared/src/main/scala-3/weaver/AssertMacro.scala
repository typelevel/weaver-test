package weaver

import scala.quoted._
import scala.language.experimental.macros
import weaver.internals.Clues

private[weaver] trait AssertMacro {

  /**
   * Asserts that a boolean value is true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  inline def apply(assertion: Clues ?=> Boolean): Unit =
    ${ AssertMacro.applyImpl('assertion) }

    /**
     * Asserts that a boolean value is true and displays a failure message if
     * not.
     *
     * Use the [[Expectations.Helpers.clue]] function to investigate any
     * failures.
     */
  inline def apply(
      assertion: Clues ?=> Boolean,
      message: => String): Unit =
    ${ AssertMacro.applyMessageImpl('assertion, 'message) }

  /**
   * Asserts that boolean values are all true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  inline def all(assertions: (Clues ?=> Boolean)*): Unit =
    ${ AssertMacro.allImpl('assertions) }
}
private[weaver] object AssertMacro {

  /**
   * Asserts that a boolean value is true.
   *
   * If the value evaluates to false, an exception is thrown.
   */
  def applyImpl[T: Type](assertion: Expr[Clues ?=> Boolean])(using
  q: Quotes): Expr[Unit] = {
    val expectations = ExpectMacro.applyImpl[T](assertion)
    '{ Assert.throwWhenFailed(${ expectations }) }
  }

  /**
   * Asserts that a boolean value is true.
   *
   * If the value evaluates to false, the message is displayed as part of the
   * thrown exception.
   */
  def applyMessageImpl[T: Type](
      assertion: Expr[Clues ?=> Boolean],
      message: => Expr[String])(using q: Quotes): Expr[Unit] = {
    val expectations = ExpectMacro.applyMessageImpl[T](assertion, message)
    '{ Assert.throwWhenFailed(${ expectations }) }
  }

  /**
   * Asserts that several boolean values are true.
   *
   * If any value evaluates to false, all generated clues are displayed as part
   * of the thrown exception.
   */
  def allImpl[T: Type](assertions: Expr[Seq[(Clues ?=> Boolean)]])(using
  q: Quotes): Expr[Unit] = {
    val expectations = ExpectMacro.allImpl[T](assertions)
    '{ Assert.throwWhenFailed(${ expectations }) }
  }

}
