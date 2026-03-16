package fix

import scalafix.v1._
import scala.meta._
class RemoveTagged extends SemanticRule("RemoveTagged") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val taggedMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#StringOps#tagged().")
    doc.tree.collect {
      case tree @ Term.Apply.After_4_6_0(Term.Select(testName, taggedMethod(_)), Term.ArgClause(List(Lit.String("ignore")), None)) =>
        Patch.replaceTree(tree, q"$testName.ignore".toString)
      case tree @ Term.Apply.After_4_6_0(Term.Select(testName, taggedMethod(_)), Term.ArgClause(List(Lit.String("only")), None)) =>
        Patch.replaceTree(tree, q"$testName.only".toString)
      case tree @ Term.Apply.After_4_6_0(Term.Select(testName, taggedMethod(_)), Term.ArgClause(List(Lit.String(other)), None)) =>
        Patch.replaceTree(tree, s"$testName")
    }.asPatch
  }

}
