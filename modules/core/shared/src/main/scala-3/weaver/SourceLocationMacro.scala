package weaver

// kudos to https://github.com/monix/minitest
// format: off

import scala.quoted._
import java.nio.file.Paths

trait SourceLocationMacro {
  trait Here {
    /**
      * Pulls source location without being affected by implicit scope.
      */
    inline def here: SourceLocation = ${macros.fromContextImpl}
  }

  implicit inline def fromContext: SourceLocation = ${macros.fromContextImpl}
}

object macros {
  def fromContextImpl(using ctx: Quotes): Expr[SourceLocation] = {
    import ctx.reflect.Position._
    import ctx.reflect._

    val pwd  = java.nio.file.Paths.get("").toAbsolutePath

    val position = Position.ofMacroExpansion
    val lineSource = position.sourceFile.content.map { content =>
      val lineContentBefore = content.slice(0, position.end).split("\\r?\\n").last
      val lineContentAfter = content.drop(position.end).split("\\r?\\n").head
      val column = lineContentBefore.length
      (s"$lineContentBefore$lineContentAfter", column)
    }
    val lineSourceExpr = Expr(lineSource)

    val psj = position.sourceFile.getJPath.getOrElse(Paths.get(position.sourceFile.path))
    // Comparing roots to workaround a Windows-specific behaviour
    // https://github.com/disneystreaming/weaver-test/issues/364
    val rp = if(pwd.getRoot == psj.getRoot) Expr(pwd.relativize(psj).toString) else Expr(psj.toString)
    val absPath = Expr(psj.toAbsolutePath.toString)
    val l = Expr(position.startLine + 1)

    '{SourceLocation($absPath, $rp, $l, $lineSourceExpr) }
  }
}

// format: on
