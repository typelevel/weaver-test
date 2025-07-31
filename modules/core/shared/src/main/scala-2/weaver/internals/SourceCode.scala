package weaver.internals

import scala.reflect.macros.blackbox

private[weaver] object SourceCode {
  // If the source contains a "$" and the user has specific scalac
  // options, the compiler will raise a "possible missing
  // interpolator" lint.
  // To work around this, turn "a$b" into List("a", b").mkString("$")
  private[weaver] def sanitize(c: blackbox.Context)(source: String): c.Tree = {
    import c.universe._
    val N = source.length - 1
    source.indexOf('$') match {
      case -1 | N => q"$source"
      case _ =>
        val parts = source.split('$').toList
        q"""${parts}.mkString("$$")"""
    }
  }

}
