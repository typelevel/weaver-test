package weaver

// kudos to https://github.com/monix/minitest
import scala.reflect.macros.blackbox
import weaver.internals.SourceCode

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
      val pwd          = java.nio.file.Paths.get("").toAbsolutePath
      val p            = c.enclosingPosition.source.path
      val abstractFile = c.enclosingPosition.source.file
      val line         = c.enclosingPosition.line
      val lineContent = SourceCode.sanitize(c)(
        c.enclosingPosition.source.lineToString(line - 1))
      val column         = c.enclosingPosition.column
      val lineSourceExpr = q"Some(($lineContent, $column))"

      // Comparing roots to workaround a Windows-specific behaviour
      // https://github.com/disneystreaming/weaver-test/issues/364
      val rp =
        if (!abstractFile.isVirtual && (pwd.getRoot() == abstractFile.file.toPath().getRoot())) {
          pwd.relativize(abstractFile.file.toPath()).toString()
        } else p

      val lineExpr = c.Expr[Int](Literal(Constant(line)))
      (p, rp, lineExpr, lineSourceExpr)
    }

  }
}
// format: on
