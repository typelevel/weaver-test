package weaver.internals

import cats.parse.{ Parser => P, Parser0 => P0 }
import cats.syntax.all.*

object TagExprParser {
  import TagExpr._

  // In GitHub syntax, space is significant (it's the AND operator)
  // So we only skip tabs, \r, \n for optional whitespace, NOT spaces
  private val nonSpaceWhitespace: P[Unit]    = P.charIn("\t\r\n").void
  private val nonSpaceWhitespaces0: P0[Unit] = nonSpaceWhitespace.rep0.void

  // Valid characters in a tag name (including colon, but not wildcards)
  private val tagChar: P[Char] = P.charIn('a' to 'z') |
    P.charIn('A' to 'Z') |
    P.charIn('0' to '9') |
    P.charIn("_-:")

  // A tag pattern can contain wildcards (* and ?) or regular characters
  private val tagPattern: P[String] = {
    val wildcardOrChar = P.charIn("*?") | tagChar
    wildcardOrChar.rep.string
  }

  private val leftParen: P[Unit] =
    P.char('(').surroundedBy(nonSpaceWhitespaces0)

  private val rightParen: P[Unit] =
    P.char(')').surroundedBy(nonSpaceWhitespaces0)

  // GitHub syntax operators
  private val andOperator: P[Unit] =
    P.char(' ').surroundedBy(nonSpaceWhitespaces0)

  private val orOperator: P[Unit] =
    P.char(',').surroundedBy(nonSpaceWhitespaces0)

  private val notOperator: P[Unit] =
    P.char('!').surroundedBy(nonSpaceWhitespaces0)

  // Forward declaration for recursive grammar
  private def expression: P[TagExpr] = P.recursive[TagExpr] { recurse =>
    // Atom: either a tag name (with optional wildcards) or parenthesized expression
    def atom: P[TagExpr] = {
      val tag = tagPattern.flatMap { pattern =>
        // If pattern contains wildcards, create a Wildcard node
        if (pattern.contains('*') || pattern.contains('?')) {
          Wildcard.parser
        } else {
          P.pure(Atom(pattern))
        }
      }
      val parens = recurse.between(leftParen, rightParen)

      (parens | tag).surroundedBy(nonSpaceWhitespaces0)
    }.withContext("atom")

    // Not expression (highest precedence)
    // In GitHub syntax: !foo or !(expr)
    val notExpr: P[TagExpr] = P.recursive[TagExpr] { recurseNot =>
      (notOperator *> recurseNot).map(Not.apply).backtrack | atom
    }.withContext("notExpr")

    // And expression (medium precedence)
    // In GitHub syntax: space is AND operator
    val andExpr: P[TagExpr] = {
      // Use rep.sep for left-associative 'and' chains
      P.repSep(notExpr, min = 1, sep = andOperator).map { exprs =>
        exprs.reduceLeft(And.apply)
      }
    }.withContext("andExpr")

    // Or expression (lowest precedence)
    // In GitHub syntax: comma is OR operator
    val orExpr: P[TagExpr] = {
      // Use rep.sep for left-associative 'or' chains
      P.repSep(andExpr, min = 1, sep = orOperator).map { exprs =>
        exprs.reduceLeft(Or.apply)
      }
    }.withContext("orExpr")

    orExpr.surroundedBy(nonSpaceWhitespaces0)
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

}
