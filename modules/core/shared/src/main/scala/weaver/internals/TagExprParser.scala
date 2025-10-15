package weaver.internals

import cats.parse.{ Parser => P, Parser0 => P0 }
import cats.syntax.show.*

object TagExprParser {
  import TagExpr._

  private val whitespace: P[Unit]    = P.charIn(" \t\r\n").void
  private val whitespaces0: P0[Unit] = whitespace.rep0.void
  private val whitespaces1: P[Unit]  = whitespace.rep.void

  private val tagName: P[String] = {
    val tagChar = P.charIn('a' to 'z') |
      P.charIn('A' to 'Z') |
      P.charIn('0' to '9') |
      P.charIn("_-")
    tagChar.rep.string
  }

  private val andKeyword: P[Unit] =
    P.string("and").surroundedBy(whitespaces0)

  private val orKeyword: P[Unit] =
    P.string("or").surroundedBy(whitespaces0)

  private val notKeyword: P[Unit] =
    P.string("not") <* whitespaces1

  private val leftParen: P[Unit] =
    P.char('(').surroundedBy(whitespaces0)

  private val rightParen: P[Unit] =
    P.char(')').surroundedBy(whitespaces0)

  // Forward declaration for recursive grammar
  private def expression: P[TagExpr] = P.recursive[TagExpr] { recurse =>
    // Atom: either a tag name or parenthesized expression
    def atom: P[TagExpr] = {
      val tag    = tagName.map(Atom.apply)
      val parens = recurse.between(leftParen, rightParen)

      (parens | tag).surroundedBy(whitespaces0)
    }.withContext("atom")

    // Not expression (highest precedence)
    val notExpr: P[TagExpr] = P.recursive[TagExpr] { recurseNot =>
      val not = (notKeyword *> recurseNot).map(Not.apply)
      not.backtrack | atom // Need backtrack here!
    }.withContext("notExpr")

    // And expression (medium precedence)
    val andExpr: P[TagExpr] = {
      // Use rep.sep for left-associative 'and' chains
      P.repSep(notExpr, min = 1, sep = andKeyword).map { exprs =>
        exprs.reduceLeft(And.apply)
      }
    }.withContext("andExpr")

    // Or expression (lowest precedence)
    val orExpr: P[TagExpr] = {
      // Use rep.sep for left-associative 'or' chains
      P.repSep(andExpr, min = 1, sep = orKeyword).map { exprs =>
        exprs.reduceLeft(Or.apply)
      }
    }.withContext("orExpr")

    orExpr.surroundedBy(whitespaces0)
  }

  def parse(input: String): Either[String, TagExpr] = {

    expression.parseAll(input) match {
      case Right(result) => Right(result)
      case Left(error)   =>
        // Extract line/column info for better error messages
        val pos         = error.failedAtOffset
        val expectation = error.expected
        val snippet     = input.take(pos + 10).drop(Math.max(0, pos - 10))
        Left(show"Parse error at position $pos near '...$snippet...', expecting one of: ${expectation}")
    }
  }

  // Helper function to validate tag names (optional)
  def isValidTagName(tag: String): Boolean = {
    tagName.parseAll(tag).isRight
  }
}
