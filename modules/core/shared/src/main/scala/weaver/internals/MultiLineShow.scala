// Adaptation of https://github.com/scalameta/munit/blob/00b41cea78bd6b253f21f0b12e6382d480279dcc/munit/shared/src/main/scala/munit/internal/console/Printers.scala
package weaver
package internals

import cats.Show

/**
 * Constructs a show instance that prints ADT and collection values on separate
 * lines.
 *
 * This is the default instance used in equality assertions.
 *
 * The Myers diff algorithm implemented in `munit-diff` operates on lines, so a
 * multi-line string is much more useful than the single-line string constructed
 * by [[Show.fromToString]].
 */
private[weaver] object MultiLineShow extends MultiLineShowCompat {

  private val open: String  = "("
  private val close: String = ")"
  private val comma: String = ","
  private val indentStep    = 1

  private[weaver] def show[A]: Show[A] = (a: A) => {
    val out = new StringBuilder()
    def loop(a: Any, indent: Int): Unit = {
      val nextIndent = indent + indentStep
      a match {
        case null => out.append("null")
        case x: Char =>
          out.append('\'')
          if (x == '\'') out.append("\\'") else printChar(x, out)
          out.append('\'')
        case x: Byte   => out.append(x.toString())
        case x: Short  => out.append(x.toString())
        case x: Int    => out.append(x.toString())
        case x: Long   => out.append(x.toString())
        case x: Float  => out.append(x.toString())
        case x: Double => out.append(x.toString())
        case x: String => printString(filterAnsi(x), out)
        case None      => out.append("None")
        case Nil       => out.append("Nil")
        case x: Map[_, _] =>
          printApply(
            collectionClassName(x),
            x.iterator,
            out,
            indent,
            nextIndent
          ) { case (key, value) =>
            loop(key, nextIndent)
            out.append(" -> ")
            loop(value, nextIndent)
          }
        case x: Iterable[_] => printApply(
            collectionClassName(x),
            x.iterator,
            out,
            indent,
            nextIndent
          )(value => loop(value, nextIndent))
        case x: Array[_] =>
          printApply("Array", x.iterator, out, indent, nextIndent)(value =>
            loop(value, nextIndent))
        case it: Iterator[_] =>
          if (it.isEmpty) out.append("empty iterator")
          else out.append("non-empty iterator")
        case p: Product =>
          val elementNames = productElementNames(p)
          val infiniteElementNames = Iterator
            .continually(if (elementNames.hasNext) elementNames.next() else "")
          printApply(
            p.productPrefix,
            p.productIterator.zip(infiniteElementNames),
            out,
            indent,
            nextIndent
          ) { case (value, key) =>
            if (key.nonEmpty) out.append(key).append(" = "): Unit
            loop(value, nextIndent)
          }
        case _ => out.append(a.toString())
      }
    }
    loop(a, indent = 0)
    out.toString()
  }

  private def printApply[T](
      prefix: String,
      it: Iterator[T],
      out: StringBuilder,
      indent: Int,
      nextIndent: Int
  )(fn: T => Unit): Unit = {
    out.append(prefix)
    out.append(open)
    if (it.hasNext) {
      printNewline(out, nextIndent)
      while (it.hasNext) {
        val value = it.next()
        fn(value)
        if (it.hasNext) {
          out.append(comma)
          printNewline(out, nextIndent)
        } else printNewline(out, indent)
      }
    }
    out.append(close)
  }

  private def printNewline(out: StringBuilder, indent: Int): Unit = {
    out.append("\n")
    Range(0, indent).foreach(out.append(' '))
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
      sb: StringBuilder
  ): Unit = c match {
    case '"'      => sb.append("\\\"")
    case '\\'     => sb.append("\\\\")
    case '\b'     => sb.append("\\b")
    case '\f'     => sb.append("\\f")
    case '\n'     => sb.append("\\n")
    case '\r'     => sb.append("\\r")
    case '\t'     => sb.append("\\t")
    case '\u001B' => ()
    case c =>
      val isNonReadableAscii = c < ' ' || c > '~'
      if (isNonReadableAscii && !Character.isLetter(c)) sb
        .append("\\u%04x".format(c.toInt))
      else sb.append(c)
  }

  private def filterAnsi(s: String): String = {
    if (s == null) {
      null
    } else {
      val len = s.length
      val r   = new java.lang.StringBuilder(len)
      var i   = 0
      while (i < len) {
        val c = s.charAt(i)
        if (c == '\u001B') {
          i += 1
          while (i < len && s.charAt(i) != 'm') i += 1
        } else {
          r.append(c)
        }
        i += 1
      }
      r.toString()
    }
  }
}
