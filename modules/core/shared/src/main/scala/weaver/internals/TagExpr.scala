package weaver.internals

import cats.parse.{ Parser0 => P0, Parser => P }
import cats.syntax.all.*

sealed trait TagExpr {
  def eval(tags: Set[String]): Boolean
}

object TagExpr {

  object Wildcard {
    /* The complexity here comes from the fact that * at the beginning or in the middle needs to
     * know the next expected parser to know when to stop consuming */
    val parser = {
      val validCharP = P.charIn('a' to 'z') |
        P.charIn('A' to 'Z') |
        P.charIn('0' to '9') |
        P.charIn("_-:")

      sealed trait Token
      case class Literal(str: String) extends Token
      case object Star                extends Token
      case object Question            extends Token

      val literalP: P[Token] =
        validCharP.rep.map(cs => Literal(cs.toList.mkString))
      val starP: P[Token]     = P.char('*').as(Star)
      val questionP: P[Token] = P.char('?').as(Question)

      val tokenP: P[Token]        = starP | questionP | literalP
      val tokensP: P[List[Token]] = tokenP.rep.map(_.toList)

      def loop(tokens: List[Token]): P0[Unit] = tokens match {
        case Nil =>
          P.unit

        case Literal(str) :: rest =>
          (P.string(str) ~ loop(rest)).void

        case Question :: rest =>
          (validCharP ~ loop(rest)).void

        case Star :: Nil =>
          // Star at the end - consume everything
          validCharP.rep0.void

        case Star :: rest =>
          // Star followed by something we need to use repUntil0 to stop before the next parser
          val afterStar = loop(rest)
          (validCharP.repUntil0(afterStar) ~ afterStar).void
      }

      tokensP.map(loop)
    }.withString.map { case (parser, pattern) =>
      Wildcard(pattern, parser)
    }

    def fromPattern(pattern: String): Either[P.Error, Wildcard] =
      parser.parseAll(pattern)

    def unsafeFromPattern(pattern: String): Wildcard =
      fromPattern(pattern).leftMap { e =>
        new RuntimeException(
          s"The pattern: ${pattern} is not a valid tag wildcard. ${e}")
      }.fold(throw _, identity)
  }

  case class Wildcard private (patternStr: String, parser: P0[Unit])
      extends TagExpr {
    def eval(tags: Set[String]): Boolean = {
      tags.exists(tag => parser.parseAll(tag).isRight)
    }

    // Override equals and hashCode to only consider patternStr, not parser
    override def equals(obj: Any): Boolean = obj match {
      case that: Wildcard => this.patternStr == that.patternStr
      case _              => false
    }

    override def hashCode(): Int = patternStr.hashCode()

    override def toString: String = s"Wildcard($patternStr)"
  }

  case class Not(expr: TagExpr) extends TagExpr {
    def eval(tags: Set[String]): Boolean = !expr.eval(tags)
  }

  case class And(left: TagExpr, right: TagExpr) extends TagExpr {
    def eval(tags: Set[String]): Boolean =
      left.eval(tags) && right.eval(tags)
  }

  case class Or(left: TagExpr, right: TagExpr) extends TagExpr {
    def eval(tags: Set[String]): Boolean =
      left.eval(tags) || right.eval(tags)
  }

}
