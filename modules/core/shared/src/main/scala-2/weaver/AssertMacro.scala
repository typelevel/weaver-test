package weaver

import scala.reflect.macros.blackbox

private[weaver] trait AssertMacro {

  /**
   * Asserts that a boolean value is true and raises an AssertionException on
   * failure.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def apply(value: Boolean): Unit = macro AssertMacro.applyImpl

  /**
   * Asserts that a boolean value is true and displays a failure message if not.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def apply(value: Boolean, message: => String): Unit =
    macro AssertMacro.messageImpl

  /**
   * Asserts that boolean values are all true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def all(values: Boolean*): Unit = macro AssertMacro.allImpl
}

private[weaver] object AssertMacro {

  /**
   * Asserts that several boolean values are true.
   *
   * If any value evaluates to false, all generated clues are displayed as part
   * of the thrown exception.
   */
  def allImpl(c: blackbox.Context)(values: c.Tree*): c.Tree = {
    import c.universe._
    val expectationsTree = ExpectMacro.allImpl(c)(values: _*)
    q"_root_.weaver.Assert.throwWhenFailed($expectationsTree)"
  }

  /**
   * Asserts that a boolean value is true.
   *
   * If the value evaluates to false, the message is displayed as part of the
   * thrown exception.
   */
  def messageImpl(c: blackbox.Context)(
      value: c.Tree,
      message: c.Tree): c.Tree = {
    import c.universe._
    val expectationsTree = ExpectMacro.messageImpl(c)(value, message)
    q"_root_.weaver.Assert.throwWhenFailed($expectationsTree)"
  }

  /**
   * Asserts that a boolean value is true.
   *
   * If the value evaluates to false, an exception is thrown.
   */
  def applyImpl(c: blackbox.Context)(value: c.Tree): c.Tree = {
    import c.universe._
    val expectationsTree = ExpectMacro.applyImpl(c)(value)
    q"_root_.weaver.Assert.throwWhenFailed($expectationsTree)"
  }
}
