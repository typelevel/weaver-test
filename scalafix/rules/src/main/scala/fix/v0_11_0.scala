package fix

import scalafix.v1._
import scala.meta._
class v0_11_0 extends SemanticRule("v0_11_0") {

  override def fix(implicit doc: SemanticDocument): Patch =
    List(renameCancelToIgnore,
         renameAssertionException,
         removeTagged,
         renameVerify).asPatch

  def renameCancelToIgnore(implicit doc: SemanticDocument): Patch = {
    val cancelMethod =
      SymbolMatcher.normalized("weaver/EffectSuite#cancel.")
    doc.tree.collect {
      case cancelMethod(tree) =>
        tree match {
          case q"cancel($reason)" =>
            Patch.replaceTree(tree, s"ignore($reason)")
          case _ =>
            Patch.empty
        }
    }.asPatch
  }

  def renameAssertionException(implicit doc: SemanticDocument): Patch = {
    val symbol = SymbolMatcher.normalized("weaver/AssertionException")
    doc.tree.collect {
      case symbol(tree) =>
        tree match {
          case q"AssertionException" =>
            Patch.replaceTree(tree, s"new ExpectationFailed")
          case t"AssertionException" =>
            Patch.replaceTree(tree, s"ExpectationFailed")
          case importee"AssertionException" =>
            Patch.replaceTree(tree, s"ExpectationFailed")
          case _ => Patch.empty
        }
    }.asPatch
  }

  def removeTagged(implicit doc: SemanticDocument): Patch = {
    val taggedMethod =
      SymbolMatcher.normalized(
        "weaver/Expectations.Helpers#StringOps#tagged().")
    doc.tree.collect {
      case tree @ Term.Apply.After_4_6_0(
            Term.Select(testName, taggedMethod(_)),
            Term.ArgClause(List(Lit.String("ignore")), None)) =>
        Patch.replaceTree(tree, q"$testName.ignore".toString)
      case tree @ Term.Apply.After_4_6_0(
            Term.Select(testName, taggedMethod(_)),
            Term.ArgClause(List(Lit.String("only")), None)) =>
        Patch.replaceTree(tree, q"$testName.only".toString)
      case tree @ Term.Apply.After_4_6_0(
            Term.Select(testName, taggedMethod(_)),
            Term.ArgClause(List(Lit.String(other)), None)) =>
        Patch.replaceTree(tree, s"$testName")
    }.asPatch
  }

  def renameVerify(implicit doc: SemanticDocument): Patch = {
    val verifyMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#verify.")
    doc.tree.collect {
      case verifyMethod(tree) => Patch.renameSymbol(tree.symbol, "expect")
    }.asPatch
  }

}
