package weaver
package framework
package test

import scala.concurrent.duration._
import weaver.internals.TagExprParser
import weaver.internals.TagExpr
import weaver.internals.TagExpr.*

object TagExprParserTests extends SimpleIOSuite {

  List(
    "foo"       -> Right(Atom("foo")),
    "(foo)"     -> Right(Atom("foo")),
    "not foo"   -> Right(Not(Atom("foo"))),
    "not (foo)" -> Right(Not(Atom("foo"))),
    // "not(foo)"           -> Right(Not(Atom("foo"))), TODO Add ( as stopword
    "not not foo"        -> Right(Not(Not(Atom("foo")))),
    "foo or bar"         -> Right(Or(Atom("foo"), Atom("bar"))),
    "foo and bar"        -> Right(And(Atom("foo"), Atom("bar"))),
    "foo or not bar"     -> Right(Or(Atom("foo"), Not(Atom("bar")))),
    "not foo or bar"     -> Right(Or(Not(Atom("foo")), Atom("bar"))),
    "not foo or not bar" -> Right(Or(Not(Atom("foo")), Not(Atom("bar")))),
    "foo or bar and foo or bar" -> Right(
      Or(
        Or(Atom("foo"),
           And(Atom("bar"), Atom("foo"))),
        Atom("bar")
      )
    ),
    "(foo or bar) and (foo or bar)" -> Right(
      And(
        Or(Atom("foo"), Atom("bar")),
        Or(Atom("foo"), Atom("bar"))
      )
    )
  ).map { case (expr, expected) =>
    pureTest(s"'$expr' should be parsed to $expected") {
      val result = TagExprParser.parse(expr)
      expect.same(expected, result)
    }
  }

  // pureTest("'foo' should be parsed to Atom('foo')") {
  //   val result = TagExprParser.parse("foo")
  //   expect.same(Right(TagExpr.Atom("foo")), result)
  // }

  // pureTest("'(foo)' should be parsed to Atom('foo')") {
  //   val result = TagExprParser.parse("(foo)")
  //   expect.same(Right(TagExpr.Atom("foo")), result)
  // }

  // pureTest("'not foo' should be parsed to Not(Atom('foo'))") {
  //   val result = TagExprParser.parse("not foo")
  //   expect.same(Right(TagExpr.Not(TagExpr.Atom("foo"))), result)
  // }

  // pureTest("'not (foo)' should be parsed to Not(Atom('foo'))") {
  //   val result = TagExprParser.parse("not (foo)")
  //   expect.same(Right(TagExpr.Not(TagExpr.Atom("foo"))), result)
  // }

  // pureTest("'(not foo)' should be parsed to Not(Atom('foo'))") {
  //   val result = TagExprParser.parse("not foo")
  //   expect.same(Right(TagExpr.Not(TagExpr.Atom("foo"))), result)
  // }

  // pureTest("'(not (foo))' should be parsed to Not(Atom('foo'))") {
  //   val result = TagExprParser.parse("not foo")
  //   expect.same(Right(TagExpr.Not(TagExpr.Atom("foo"))), result)
  // }

  // pureTest("'not not foo' should be parsed to Not(Not(Atom('foo')))") {
  //   val result = TagExprParser.parse("not not foo")
  //   expect.same(Right(TagExpr.Not(TagExpr.Not(TagExpr.Atom("foo")))), result)
  // }
}
