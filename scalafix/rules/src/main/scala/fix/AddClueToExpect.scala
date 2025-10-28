package fix

import scalafix.v1._
import scala.meta._

class AddClueToExpect extends SemanticRule("AddClueToExpect") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val expectMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#expect.")
    doc.tree.collect {
      case expectMethod(tree) =>
        val clues = tree.collect {
          case q"clue" => ()
        }
        if (clues.isEmpty) {
          tree match {
            case q"expect($lhs $op $rhs)" =>
              Patch.replaceTree(
                tree,
                s"expect(${makeClue(lhs)} $op ${makeClue(rhs)})")
            case q"expect($lhs.$op(...$exprss))" =>
              val clues = exprss.map(_.map(makeClue))
              Patch.replaceTree(
                tree,
                q"expect(${makeClue(lhs)}.$op(...$clues))".toString)
            case q"expect($op(...$exprss))" =>
              val clues = exprss.map(_.map(makeClue))
              Patch.replaceTree(tree, q"expect($op(...$clues))".toString)
            case q"expect" =>
              Patch.empty
            case other =>
              println(s"Ignoring $other")
              Patch.empty
          }
        } else {
          Patch.empty
        }
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
}
