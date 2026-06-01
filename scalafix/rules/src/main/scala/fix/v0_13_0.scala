package fix

import scalafix.v1._
import scala.meta._

class v0_13_0 extends SemanticRule("v0_13_0") {

  override def fix(implicit doc: SemanticDocument): Patch =
    renameMutableSuites + renameSuiteName

  def renameMutableSuites(implicit doc: SemanticDocument): Patch = {
    val suiteNames = Map(
      "MutableIOSuite"       -> "IOSuite",
      "MutableFSuite"        -> "FSuite",
      "SimpleMutableIOSuite" -> "SimpleIOSuite",
      "FunSuiteIO"           -> "FunSuite"
    )
    suiteNames.map { case (prevSuiteName, nextSuiteName) =>
      val suiteSymbol = SymbolMatcher.normalized(s"weaver/$prevSuiteName")
      doc.tree.collect {
        case suiteSymbol(tree) =>
          tree match {
            case Type.Name(`prevSuiteName`) =>
              Patch.replaceTree(tree, nextSuiteName)
            case Importee.Name(_) =>
              Patch.replaceTree(tree, nextSuiteName)
            case Importee.Rename(fromTree, _) =>
              Patch.replaceTree(fromTree, nextSuiteName)
            case _ =>
              Patch.empty
          }
      }.asPatch
    }.asPatch
  }

  def renameSuiteName(implicit doc: SemanticDocument): Patch = {
    val symbol = SymbolMatcher.normalized(s"weaver/EffectSuite#name().")
    doc.tree.collect {
      case symbol(tree) =>
        tree match {
          case Term.Name("name") =>
            Patch.replaceTree(tree, "suiteName")
          case _ => Patch.empty
        }
    }.asPatch
  }
}
