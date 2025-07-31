package weaver.internals

import scala.annotation.compileTimeOnly
import org.typelevel.scalaccompat.annotation.unused

private[weaver] trait ClueHelpers {

  /**
   * Used to investigate failures in `expect` statements.
   *
   * Surround a value with a call to `clue` to display it on failure.
   */
  @compileTimeOnly(
    "This function can only be used within `expect`.")
  final def clue[A](@unused a: Clue[A]): A = {
    // This function is removed as part of the `expect` macro expansion.
    throw new Error("compileTimeOnly annotation not respected! This is likely to be a bug in weaver-test. Report it at https://github.com/typelevel/weaver-test/issues/new")
  }
}
