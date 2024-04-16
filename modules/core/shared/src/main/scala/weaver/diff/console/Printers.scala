// Adaptation of https://github.com/lihaoyi/PPrint/blob/e6a918c259ed7ae1998bbf58c360334a3f0157ca/pprint/src/pprint/Walker.scala
package weaver.diff.console

import scala.annotation.switch

object Printers {

  /**
   * Pretty-prints the value in a format that's optimized for producing diffs
   */
  def print(any: String): String = {
    val out = new StringBuilder()
    printString(any, out)
    weaver.diff.console.AnsiColors.filterAnsi(out.toString())
  }

  private def printString(
      string: String,
      out: StringBuilder
  ): Unit = {
    val isMultiline = string.contains('\n')

    if (isMultiline) {
      out.append('"')
      out.append('"')
      out.append('"')
      out.append(string)
      out.append('"')
      out.append('"')
      out.append('"')
    } else {
      out.append('"')
      var i = 0
      while (i < string.length()) {
        printChar(string.charAt(i), out)
        i += 1
      }
      out.append('"')
    }
  }

  private def printChar(
      c: Char,
      sb: StringBuilder,
      isEscapeUnicode: Boolean = true
  ): Unit =
    (c: @switch) match {
      case '"'  => sb.append("\\\"")
      case '\\' => sb.append("\\\\")
      case '\b' => sb.append("\\b")
      case '\f' => sb.append("\\f")
      case '\n' => sb.append("\\n")
      case '\r' => sb.append("\\r")
      case '\t' => sb.append("\\t")
      case c =>
        val isNonReadableAscii = c < ' ' || (c > '~' && isEscapeUnicode)
        if (isNonReadableAscii && !Character.isLetter(c))
          sb.append("\\u%04x".format(c.toInt))
        else sb.append(c)
    }

}
