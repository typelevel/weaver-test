package weaver.internals

import scala.reflect.macros.blackbox.Context

// This code is heavily borrowed from munit's Clue macro: https://github.com/scalameta/munit/blob/426e79708accb5b7136689d781f7593b473589f4/munit/shared/src/main/scala/munit/internal/MacroCompatScala2.scala#L25
private[weaver] object ClueMacro {
  def showFromToStringImpl(c: Context)(value: c.Tree): c.Tree = {
    import c.universe._
    impl(c)(value)(q"cats.Show.fromToString[${value.tpe}]")
  }

  /**
   * Constructs a clue by extracting the source code and type information of a
   * value.
   */
  def impl(c: Context)(value: c.Tree)(catsShow: c.Tree): c.Tree = {
    import c.universe._
    val text: String =
      if (value.pos != null && value.pos.isRange) {
        val chars = value.pos.source.content
        val start = value.pos.start
        val end   = value.pos.end
        if (end > start &&
          start >= 0 && start < chars.length &&
          end >= 0 && end < chars.length) {
          new String(chars, start, end - start)
        } else {
          ""
        }
      } else {
        ""
      }
    def simplifyType(tpe: Type): Type = tpe match {
      case TypeRef(ThisType(pre), sym, args) if pre == sym.owner =>
        simplifyType(c.internal.typeRef(NoPrefix, sym, args))
      case t =>
        t.widen
    }
    val source    = Literal(Constant(text.trim))
    val valueType = Literal(Constant(simplifyType(value.tpe).toString()))
    val clueTpe   = c.internal.typeRef(
      NoPrefix,
      c.mirror.staticClass(classOf[Clue[_]].getName()),
      List(value.tpe.widen)
    )
    q"new $clueTpe(..$source, $value, $valueType, $catsShow)"
  }
}
