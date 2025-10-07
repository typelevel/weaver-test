package fix

import scalafix.v1._
import scala.meta._
class RenameAssertionExceptionToExpectationFailed extends SemanticRule("RenameAssertionExceptionToExpectationFailed") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val symbol = SymbolMatcher.normalized("weaver/AssertionException")
    doc.tree.collect {
      case symbol(tree) =>
        tree match {
          case q"AssertionException" => Patch.replaceTree(tree, s"new ExpectationFailed")
          case t"AssertionException" => Patch.replaceTree(tree, s"ExpectationFailed")
          case importee"AssertionException" => Patch.replaceTree(tree, s"ExpectationFailed")
          case _ => Patch.empty
        }
    }.asPatch
  }

}
