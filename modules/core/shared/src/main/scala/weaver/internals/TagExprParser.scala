package weaver.internals

import cats.parse.{ Parser, Parser0 }
import cats.syntax.all.*

private[weaver] object TagExprParser {
  import TagExpr._

  // In GitHub syntax, space is significant (it's the AND operator)
  // So we only skip tabs, \r, \n for optional whitespace, NOT spaces
  private val nonSpaceWhitespace: Parser[Unit]    = Parser.charIn("\t\r\n").void
  private val nonSpaceWhitespaces0: Parser0[Unit] = nonSpaceWhitespace.rep0.void

  private val leftParen: Parser[Unit] =
    Parser.char('(').surroundedBy(nonSpaceWhitespaces0)

  private val rightParen: Parser[Unit] =
    Parser.char(')').surroundedBy(nonSpaceWhitespaces0)

  private val andOperator: Parser[Unit] =
    Parser.char(' ').surroundedBy(nonSpaceWhitespaces0)

  private val orOperator: Parser[Unit] =
    Parser.char(',').surroundedBy(nonSpaceWhitespaces0)

  private val notOperator: Parser[Unit] =
    Parser.char('!').surroundedBy(nonSpaceWhitespaces0)

  // Forward declaration for recursive grammar
  private val expression: Parser[TagExpr] = Parser.recursive[TagExpr] {
    recurse =>
      // either a tag name (with optional wildcards) or parenthesized expression
      val wildcardP: Parser[TagExpr] = {
        val parens = recurse.between(leftParen, rightParen)

        (parens | Wildcard.parser).surroundedBy(nonSpaceWhitespaces0)
      }.withContext("wildcard")

      // Not expression (highest precedence)
      // In GitHub syntax: !foo or !(expr)
      val notExprP: Parser[TagExpr] = Parser.recursive[TagExpr] { recurseNot =>
        (notOperator *> recurseNot).map(Not.apply).backtrack | wildcardP
      }.withContext("notExpr")

      // And expression (medium precedence)
      // In GitHub syntax: space is AND operator
      val andExprP: Parser[TagExpr] = {
        // Use rep.sep for left-associative 'and' chains
        Parser.repSep(notExprP, min = 1, sep = andOperator).map { exprs =>
          exprs.reduceLeft(And.apply)
        }
      }.withContext("andExpr")

      // Or expression (lowest precedence)
      // In GitHub syntax: comma is OR operator
      val orExprP: Parser[TagExpr] = {
        // Use rep.sep for left-associative 'or' chains
        Parser.repSep(andExprP, min = 1, sep = orOperator).map { exprs =>
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
