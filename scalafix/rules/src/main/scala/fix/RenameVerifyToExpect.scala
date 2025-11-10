package fix

import scalafix.v1._

class RenameVerifyToExpect extends SemanticRule("RenameVerifyToExpect") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val assertMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#verify.")
    doc.tree.collect {
      case assertMethod(tree) => Patch.renameSymbol(tree.symbol, "expect")
    }.asPatch
  }

}
