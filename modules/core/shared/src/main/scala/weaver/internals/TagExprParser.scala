package weaver.internals

import cats.parse.{ Parser => P, Parser0 => P0 }
import cats.syntax.all.*

object TagExprParser {
  import TagExpr._

  // In GitHub syntax, space is significant (it's the AND operator)
  // So we only skip tabs, \r, \n for optional whitespace, NOT spaces
  private val nonSpaceWhitespace: P[Unit]    = P.charIn("\t\r\n").void
  private val nonSpaceWhitespaces0: P0[Unit] = nonSpaceWhitespace.rep0.void

  private val leftParen: P[Unit] =
    P.char('(').surroundedBy(nonSpaceWhitespaces0)

  private val rightParen: P[Unit] =
    P.char(')').surroundedBy(nonSpaceWhitespaces0)

  private val andOperator: P[Unit] =
    P.char(' ').surroundedBy(nonSpaceWhitespaces0)

  private val orOperator: P[Unit] =
    P.char(',').surroundedBy(nonSpaceWhitespaces0)

  private val notOperator: P[Unit] =
    P.char('!').surroundedBy(nonSpaceWhitespaces0)

  // Forward declaration for recursive grammar
  private def expression: P[TagExpr] = P.recursive[TagExpr] { recurse =>
    // either a tag name (with optional wildcards) or parenthesized expression
    val wildcardP: P[TagExpr] = {
      val parens = recurse.between(leftParen, rightParen)

      (parens | Wildcard.parser).surroundedBy(nonSpaceWhitespaces0)
    }.withContext("wildcard")

    // Not expression (highest precedence)
    // In GitHub syntax: !foo or !(expr)
    val notExprP: P[TagExpr] = P.recursive[TagExpr] { recurseNot =>
      (notOperator *> recurseNot).map(Not.apply).backtrack | wildcardP
    }.withContext("notExpr")

    // And expression (medium precedence)
    // In GitHub syntax: space is AND operator
    val andExprP: P[TagExpr] = {
      // Use rep.sep for left-associative 'and' chains
      P.repSep(notExprP, min = 1, sep = andOperator).map { exprs =>
        exprs.reduceLeft(And.apply)
      }
    }.withContext("andExpr")

    // Or expression (lowest precedence)
    // In GitHub syntax: comma is OR operator
    val orExprP: P[TagExpr] = {
      // Use rep.sep for left-associative 'or' chains
      P.repSep(andExprP, min = 1, sep = orOperator).map { exprs =>
        exprs.reduceLeft(Or.apply)
      }
    }.withContext("orExpr")

    orExprP.surroundedBy(nonSpaceWhitespaces0)
  }

  def parse(input: String): Either[String, TagExpr] = {

    expression.parseAll(input) match {
      case Right(result) => Right(result)
      case Left(error) =>
        val pos         = error.failedAtOffset
        val expectation = error.expected
        val snippet     = input.take(pos + 10).drop(Math.max(0, pos - 10))
        Left(show"Parse error at position $pos near '...$snippet...', expecting one of: ${expectation}")
    }
  }

}
