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
    val content = position.sourceFile.content.map { content =>
      val startSlice = content.slice(0, position.end).split("\n").last
      val column = startSlice.length
      val endSlice = content.drop(position.end).takeWhile(char => char != '\n')
      (s"$startSlice$endSlice", column)
    }
    val sourceCode = Expr(content)

    val psj = position.sourceFile.getJPath.getOrElse(Paths.get(position.sourceFile.path))
    // Comparing roots to workaround a Windows-specific behaviour
    // https://github.com/disneystreaming/weaver-test/issues/364
    val rp = if(pwd.getRoot == psj.getRoot) Expr(pwd.relativize(psj).toString) else Expr(psj.toString)
    val absPath = Expr(psj.toAbsolutePath.toString)
    val l = Expr(position.startLine + 1)

    '{new SourceLocation($absPath, $rp, $l, $sourceCode) }
  }
}

// format: on
