package weaver.internals

import cats.parse.{ Parser0 => P0, Parser => P }
import cats.syntax.all.*

sealed trait TagExpr {
  def eval(tags: Set[String]): Boolean
}

object TagExpr {

  case class Atom(name: String) extends TagExpr {
    def eval(tags: Set[String]): Boolean = tags.contains(name)
  }

  object Wildcard {
    val parser = {
      val validCharP = P.charIn('a' to 'z') |
        P.charIn('A' to 'Z') |
        P.charIn('0' to '9') |
        P.charIn("_-:")
      val literalP: P[P[String]] = validCharP.rep.map { cs =>
        val str = cs.mkString_("")
        P.string(str).as(str)
      }
      val questionMarkP: P[P[String]] = P.char('?').map { _ =>
        validCharP.map(_.toString)
      }
      val starP: P[P0[String]] = P.char('*').map { _ =>
        validCharP.rep0.string
      }

      (starP | questionMarkP | literalP).rep.map { parts =>
        parts.reduceLeft { (acc, next) =>
          (acc, next).mapN(_ + _)
        }
      }
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

  case class Wildcard private (patternStr: String, parser: P0[String])
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
