package weaver

import scala.reflect.macros.blackbox
import weaver.internals.ClueHelpers
import weaver.internals.SourceCode

private[weaver] trait ExpectMacro {

  /**
   * Asserts that a boolean value is true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def apply(value: Boolean)(implicit loc: SourceLocation): Expectations =
    macro ExpectMacro.applyImpl

  /**
   * Asserts that a boolean value is true and displays a failure message if not.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def apply(value: Boolean, message: => String)(implicit
      loc: SourceLocation): Expectations =
    macro ExpectMacro.messageImpl

  /**
   * Asserts that boolean values are all true.
   *
   * Use the [[Expectations.Helpers.clue]] function to investigate any failures.
   */
  def all(values: Boolean*)(implicit loc: SourceLocation): Expectations =
    macro ExpectMacro.allImpl
}

private[weaver] object ExpectMacro {

  /**
   * Constructs [[Expectations]] from several boolean values.
   *
   * If any value evaluates to false, all generated clues are displayed as part
   * of the failed expectation.
   */
  def allImpl(c: blackbox.Context)(values: c.Tree*)(
      loc: c.Tree): c.Tree = {
    import c.universe._
    val clueMethodSymbol = getClueMethodSymbol(c)
    val allExpectations = values.toList.map { value =>
      val (cluesName, cluesValDef) = makeClues(c)
      val transformedValue =
        replaceClueMethodCalls(c)(clueMethodSymbol, cluesName, value)
      val sourceCode =
        new String(value.pos.source.content.slice(value.pos.start,
                                                  value.pos.end))
      makeExpectations(c)(cluesName = cluesName,
                          cluesValDef = cluesValDef,
                          value = transformedValue,
                          loc = loc,
                          sourceCode = sourceCode,
                          message = q"None")
    }
    q"List(..$allExpectations).reduce(_ and _)"
  }

  /**
   * Constructs [[Expectations]] from a boolean value and message.
   *
   * If the value evaluates to false, the message is displayed as part of the
   * failed expectation.
   */
  def messageImpl(c: blackbox.Context)(
      value: c.Tree,
      message: c.Tree)(loc: c.Tree): c.Tree = {
    import c.universe._
    val sourcePos = c.enclosingPosition
    val sourceCode =
      new String(sourcePos.source.content.slice(sourcePos.start, sourcePos.end))
    val (cluesName, cluesValDef) = makeClues(c)
    val clueMethodSymbol         = getClueMethodSymbol(c)

    val transformedValue =
      replaceClueMethodCalls(c)(clueMethodSymbol, cluesName, value)
    makeExpectations(c)(cluesName = cluesName,
                        cluesValDef = cluesValDef,
                        value = transformedValue,
                        loc = loc,
                        sourceCode = sourceCode,
                        message = q"Some($message)")
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
  def applyImpl(c: blackbox.Context)(value: c.Tree)(
      loc: c.Tree): c.Tree = {

    import c.universe._
    val sourcePos = c.enclosingPosition
    val sourceCode =
      new String(sourcePos.source.content.slice(sourcePos.start, sourcePos.end))

    val (cluesName, cluesValDef) = makeClues(c)
    val clueMethodSymbol         = getClueMethodSymbol(c)

    val transformedValue =
      replaceClueMethodCalls(c)(clueMethodSymbol, cluesName, value)
    makeExpectations(c)(cluesName = cluesName,
                        cluesValDef = cluesValDef,
                        value = transformedValue,
                        loc = loc,
                        sourceCode = sourceCode,
                        message = q"None")
  }

  /** Constructs [[Expectations]] from the local [[Clues]] collection. */
  private def makeExpectations(c: blackbox.Context)(
      cluesName: c.TermName,
      cluesValDef: c.Tree,
      value: c.Tree,
      loc: c.Tree,
      sourceCode: String,
      message: c.Tree): c.Tree = {
    import c.universe._
    val sanitizedSourceCode = SourceCode.sanitize(c)(sourceCode)
    val block =
      q"$cluesValDef; _root_.weaver.internals.Clues.toExpectations($loc, Some($sanitizedSourceCode), $message, $cluesName, $value)"
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

    // This transformation outputs code that adds clues to a local
    // clues collection `cluesName`. It recurses over the input code and replaces
    // all calls of `ClueHelpers.clue` with `cluesName.addClue`.
    object transformer extends Transformer {

      override def transform(input: Tree): Tree = input match {
        case c.universe.Apply(fun, List(clueValue))
            if fun.symbol == clueMethodSymbol =>
          // The input tree corresponds to `ClueHelpers.clue(clueValue)` .
          // Transform it into `clueName.addClue(clueValue)`
          // Apply the transformation recursively to `clueValue` to support nested clues.
          val transformedClueValue = super.transform(clueValue)
          q"""${cluesName}.addClue($transformedClueValue)"""
        case o =>
          // Otherwise, recurse over the input.
          super.transform(o)
      }
    }

    transformer.transform(value)
  }
}
