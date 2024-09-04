package weaver.internals

import cats.Show

/**
 * Captures the source code, type information, and runtime representation of a
 * value.
 *
 * Clues are useful for investigating failed assertions. A clue for a given
 * value is summoned with the [[ClueHelpers.clue]] function. This constructs a
 * clue for a given value using an implicit conversion.
 *
 * @param source
 *   The source code of the value
 * @param value
 *   The runtime value
 * @param valueType
 *   The string representation of the type of the value
 * @param show
 *   The [[cats.Show]] typeclass used to display the value.
 */
private[weaver] class Clue[T](
    source: String,
    private[internals] val value: T,
    valueType: String,
    show: Show[T]
) {
  private[internals] def prettyPrint: String =
    s"${source}: ${valueType} = ${show.show(value)}"
}

private[internals] trait LowPriorityClueImplicits {

  /**
   * Generates a clue for a given value using the [[toString]] function to print
   * the value.
   */
  implicit def generateClueFromToString[A](value: A): Clue[A] =
    macro ClueMacro.showFromToStringImpl
}
private[weaver] object Clue extends LowPriorityClueImplicits {

  /**
   * Generates a clue for a given value using a [[Show]] instance to print the
   * value.
   */
  implicit def generateClue[A](value: A)(implicit catsShow: Show[A]): Clue[A] =
    macro ClueMacro.impl

}
