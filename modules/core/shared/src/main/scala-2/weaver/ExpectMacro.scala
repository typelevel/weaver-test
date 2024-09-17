package weaver

import scala.reflect.macros.blackbox
import weaver.internals.ClueHelpers

private[weaver] trait ExpectMacro {

  /**
   * Asserts that a boolean value is true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def apply(value: Boolean): Expectations = macro ExpectMacro.applyImpl

  /**
   * Asserts that a boolean value is true and displays a failure message if not.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def apply(value: Boolean, message: => String): Expectations =
    macro ExpectMacro.messageImpl

  /**
   * Asserts that boolean values are all true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def all(values: Boolean*): Expectations = macro ExpectMacro.allImpl
}

private[weaver] object ExpectMacro {

  /**
   * Constructs [[Expectations]] from several boolean values.
   *
   * If any value evaluates to false, all generated clues are displayed as part
   * of the failed expectation.
   */
  def allImpl(c: blackbox.Context)(values: c.Tree*): c.Tree = {
    import c.universe._
    val sourceLoc = new weaver.macros.Macros(c).fromContext.asInstanceOf[c.Tree]
    val (cluesName, cluesValDef) = makeClues(c)
    val clueMethodSymbol         = getClueMethodSymbol(c)

    val transformedValues =
      values.toList.map(replaceClueMethodCalls(c)(clueMethodSymbol,
                                                  cluesName,
                                                  _))
    makeExpectations(c)(cluesName,
                        cluesValDef,
                        transformedValues,
                        sourceLoc,
                        q"None")
  }

  /**
   * Constructs [[Expectations]] from a boolean value and message.
   *
   * If the value evaluates to false, the message is displayed as part of the
   * failed expectation.
   */
  def messageImpl(c: blackbox.Context)(
      value: c.Tree,
      message: c.Tree): c.Tree = {
    import c.universe._
    val sourceLoc = new weaver.macros.Macros(c).fromContext.asInstanceOf[c.Tree]
    val (cluesName, cluesValDef) = makeClues(c)
    val clueMethodSymbol         = getClueMethodSymbol(c)

    val transformedValue =
      replaceClueMethodCalls(c)(clueMethodSymbol, cluesName, value)
    makeExpectations(c)(cluesName,
                        cluesValDef,
                        List(transformedValue),
                        sourceLoc,
                        q"Some($message)")
  }

  /**
   * Constructs [[Expectations]] from a boolean value.
   *
   * A macro is needed to support clues. The value expression may contain calls
   * to [[ClueHelpers.clue]], which generate clues for values under test.
   *
   * This macro constructs a local collection of [[Clues]] and adds the
   * generated clues to it. Calls to [[ClueHelpers.clue]] are rewritten to calls
   * to [[Clues.addClue]].
   *
   * After the value is evaluated, the [[Clues]] collection is used to contruct
   * [[Expectations]].
   */
  def applyImpl(c: blackbox.Context)(value: c.Tree): c.Tree = {

    import c.universe._
    val sourceLoc = new weaver.macros.Macros(c).fromContext.asInstanceOf[c.Tree]
    val (cluesName, cluesValDef) = makeClues(c)
    val clueMethodSymbol         = getClueMethodSymbol(c)

    val transformedValue =
      replaceClueMethodCalls(c)(clueMethodSymbol, cluesName, value)
    makeExpectations(c)(cluesName,
                        cluesValDef,
                        List(transformedValue),
                        sourceLoc,
                        q"None")
  }

  /** Constructs [[Expectations]] from the local [[Clues]] collection. */
  private def makeExpectations(c: blackbox.Context)(
      cluesName: c.TermName,
      cluesValDef: c.Tree,
      values: List[c.Tree],
      sourceLoc: c.Tree,
      message: c.Tree): c.Tree = {
    import c.universe._
    val block =
      q"$cluesValDef; _root_.weaver.internals.Clues.toExpectations($sourceLoc, $message, $cluesName, ..$values)"
    val untyped = c.untypecheck(block)
    val retyped = c.typecheck(untyped, pt = c.typeOf[Expectations])
    retyped

  }

  /** Get the [[ClueHelpers.clue]] symbol. */
  private def getClueMethodSymbol(c: blackbox.Context): c.Symbol = {
    import c.universe._
    symbolOf[ClueHelpers].info.member(TermName("clue"))
  }

  /** Construct a [[Clues]] collection local to the `expect` call. */
  private def makeClues(c: blackbox.Context): (c.TermName, c.Tree) = {
    import c.universe._
    val cluesName = TermName(c.freshName("clues$"))
    val cluesValDef =
      q"val $cluesName: _root_.weaver.internals.Clues = new _root_.weaver.internals.Clues()"
    (cluesName, cluesValDef)
  }

  /**
   * Replaces all calls to [[ClueHelpers.clue]] with calls to [[Clues.addClue]].
   */
  private def replaceClueMethodCalls(c: blackbox.Context)(
      clueMethodSymbol: c.Symbol,
      cluesName: c.TermName,
      value: c.Tree): c.Tree = {

    import c.universe._
    object transformer extends Transformer {

      override def transform(input: Tree): Tree = input match {
        case c.universe.Apply(fun, List(clueValue))
            if fun.symbol == clueMethodSymbol =>
          val transformedClueValue = super.transform(clueValue)
          val clueName             = TermName(c.freshName("clue$"))
          q"""{val $clueName = ${transformedClueValue}; ${cluesName}.addClue($clueName)}"""
        case o => super.transform(o)
      }
    }

    transformer.transform(value)
  }
}
