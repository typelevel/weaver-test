package fix

import scalafix.v1._
import scala.meta._
class RenameCancelToIgnore extends SemanticRule("RenameCancelToIgnore") {

  override def fix(implicit doc: SemanticDocument): Patch = {
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

}
