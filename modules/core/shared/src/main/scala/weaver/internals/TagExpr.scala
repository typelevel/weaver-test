package weaver.internals

sealed trait TagExpr {
  def eval(tags: Set[String]): Boolean
}

object TagExpr {
  case class Atom(name: String) extends TagExpr {
    def eval(tags: Set[String]): Boolean = tags.contains(name)
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
