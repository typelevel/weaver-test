package fix

import scalafix.v1._
import scala.meta._
class RenameExpectToExpectSame
    extends SemanticRule("RenameExpectToExpectSame") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val expectMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#expect.")
    doc.tree.collect {
      case expectMethod(tree) =>
        tree match {
          case q"expect($lhs == $rhs)" =>
            Patch.replaceTree(tree, s"expect.same($lhs, $rhs)")
          case _ =>
            Patch.empty
        }
    }.asPatch
  }

}
