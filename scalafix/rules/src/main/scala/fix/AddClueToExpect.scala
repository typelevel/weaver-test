package fix

import scalafix.v1._
import scala.meta._

class AddClueToExpect extends SemanticRule("AddClueToExpect") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val expectMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#expect.")
    val expectAllMethod = SymbolMatcher.normalized("weaver/ExpectMacro#all().")

    doc.tree.collect {
      case Term.Apply.After_4_6_0(expectMethod(_),
                                  Term.ArgClause(List(tree), _)) =>
        Patch.replaceTree(tree, addClues(tree).toString)
      case Term.Apply.After_4_6_0(expectAllMethod(_),
                                  Term.ArgClause(trees, _)) =>
        trees.map { tree =>
          Patch.replaceTree(tree, addClues(tree).toString)
        }.asPatch
    }.asPatch
  }

  private def makeClue(expr: Term)(implicit doc: SemanticDocument): Term = {
    expr match {
      case _: Lit                    => expr
      case _: Term.Function          => expr
      case _: Term.AnonymousFunction => expr
      case _                         => q"clue($expr)"
    }
  }

  private def addClues(tree: Tree)(implicit doc: SemanticDocument): Tree = {
    val clueSymbol =
      SymbolMatcher.normalized("weaver/internals/ClueHelpers#clue().")
    val clues = tree.collect {
      case clueSymbol(_) => ()
    }
    if (clues.isEmpty) {
      tree match {
        case q"$lhs $op $rhs" =>
          q"${makeClue(lhs)} $op ${makeClue(rhs)}"
        case q"$lhs.$op(...$exprss)" =>
          val clues = exprss.map(_.map(makeClue))
          q"${makeClue(lhs)}.$op(...$clues)"
        case q"!$lhs.$op(...$exprss)" =>
          val clues = exprss.map(_.map(makeClue))
          q"!${makeClue(lhs)}.$op(...$clues)"
        case q"$expr.$op[..$tpes]" =>
          q"${makeClue(expr)}.$op[..$tpes]"
        case q"!$expr.$op[..$tpes]" =>
          q"!${makeClue(expr)}.$op[..$tpes]"
        case q"!$op(...$exprss)" =>
          val clues = exprss.map(_.map(makeClue))
          q"!$op(...$clues)"
        case q"$op(...$exprss)" =>
          val clues = exprss.map(_.map(makeClue))
          q"$op(...$clues)"
        case other => other
      }
    } else {
      tree
    }
  }
}
