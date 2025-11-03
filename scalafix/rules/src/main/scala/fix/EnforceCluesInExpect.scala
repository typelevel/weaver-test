package fix

import scalafix.v1._
import scala.meta._

class EnforceCluesInExpect extends SemanticRule("EnforceCluesInExpect") {

  import EnforceCluesInExpect._
  override def fix(implicit doc: SemanticDocument): Patch = {
    val expectMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#expect.")
    val expectAllMethod = SymbolMatcher.normalized("weaver/ExpectMacro#all().")

    doc.tree.collect {
      case Term.Apply.After_4_6_0(expectMethod(_),
                                  Term.ArgClause(List(tree), _)) =>
        enforceCluesOrRename(tree)
      case Term.Apply.After_4_6_0(expectAllMethod(_),
                                  Term.ArgClause(trees, _)) =>
        trees.map(enforceCluesOrRename).asPatch
    }.asPatch
  }

  private def enforceCluesOrRename(tree: Tree)(implicit
      doc: SemanticDocument): Patch = {
    if (hasClues(tree)) {
      Patch.empty
    } else {
      tree match {
        case q"$lhs == $rhs"  => Patch.lint(RenameToExpectSame(tree.pos))
        case q"$lhs === $rhs" => Patch.lint(RenameToExpectEql(tree.pos))
        case _ if cluesAreUseful(tree) => Patch.lint(AddClue(tree.pos))
        case _                         => Patch.empty
      }
    }
  }

  private def hasClues(tree: Tree)(implicit doc: SemanticDocument): Boolean = {
    val clueSymbol =
      SymbolMatcher.normalized("weaver/internals/ClueHelpers#clue().")
    tree.collect {
      case clueSymbol(_) => ()
    }.nonEmpty
  }

  private def cluesAreUseful(tree: Tree): Boolean = {
    tree match {
      case Term.Name(_) =>
        // Clues are not useful for names e.g. `expect(exists)` where `exists` is a boolean value.
        false
      case Term.ApplyUnary(Term.Name("!"), Term.Name(_)) =>
        // Clues are not useful for negated names e.g. `expect(!exists)` where `exists` is a boolean value.
        false
      case _ => true
    }
  }
}

object EnforceCluesInExpect {

  case class RenameToExpectSame(position: Position) extends Diagnostic {
    def message =
      "equality assertion should use `expect.eql` or `expect.same`. Read https://typelevel.org/weaver-test/features/asserting_equality.html for more details."
  }

  case class RenameToExpectEql(position: Position) extends Diagnostic {
    def message =
      "equality assertion should use `expect.eql`. Read https://typelevel.org/weaver-test/features/asserting_equality.html for more details."
  }

  case class AddClue(position: Position) extends Diagnostic {
    def message =
      "assertion must contain clues. Read https://typelevel.org/weaver-test/features/troubleshooting_failures.html#using-clue-with-expect for more details."
  }
}
