package weaver

// kudos to https://github.com/monix/minitest
// format: off
import scala.reflect.macros.blackbox

trait SourceLocationMacro {

  import macros._

  trait Here {
    /**
      * Pulls source location without being affected by implicit scope.
      */
    def here: SourceLocation = macro Macros.fromContext
  }

  implicit def fromContext: SourceLocation =
    macro Macros.fromContext


}

object macros {
  class Macros(val c: blackbox.Context) {
    import c.universe._

    def fromContext: c.Tree = {
      val (pathExpr, relPathExpr, lineExpr, lineSourceExpr) = getSourceLocation
      val SourceLocationSym = symbolOf[SourceLocation].companion
      q"""$SourceLocationSym($pathExpr, $relPathExpr, $lineExpr, $lineSourceExpr)"""
    }

    private def getSourceLocation = {
      val pwd  = java.nio.file.Paths.get("").toAbsolutePath
      val p = c.enclosingPosition.source.path
      val abstractFile = c.enclosingPosition.source.file
      val lineSource = c.enclosingPosition.source.lineToString(c.enclosingPosition.line - 1)
      val lineSourceExpr = if (lineSource.trim.isEmpty) q"None" else q"Some($lineSource)"

      // Comparing roots to workaround a Windows-specific behaviour
      // https://github.com/disneystreaming/weaver-test/issues/364
      val rp = if (!abstractFile.isVirtual && (pwd.getRoot() == abstractFile.file.toPath().getRoot())){
        pwd.relativize(abstractFile.file.toPath()).toString()
      } else p

      val line = c.Expr[Int](Literal(Constant(c.enclosingPosition.line)))
      (p, rp, line, lineSourceExpr)
    }

  }
}
// format: on
