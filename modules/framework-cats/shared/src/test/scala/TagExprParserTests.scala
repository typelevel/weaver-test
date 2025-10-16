package weaver
package framework
package test

import weaver.internals.TagExprParser
import weaver.internals.TagExpr.*

object TagExprParserTests extends SimpleIOSuite {

  List(
    // Basic atom
    "foo"   -> Right(Atom("foo")),
    "(foo)" -> Right(Atom("foo")),

    // NOT expressions
    "!foo"   -> Right(Not(Atom("foo"))),
    "!(foo)" -> Right(Not(Atom("foo"))),
    "!!foo"  -> Right(Not(Not(Atom("foo")))),

    // OR expressions
    "foo,bar" -> Right(Or(Atom("foo"), Atom("bar"))),

    // AND expressions
    "foo bar" -> Right(And(Atom("foo"), Atom("bar"))),

    // Combined expressions
    "foo,!bar"  -> Right(Or(Atom("foo"), Not(Atom("bar")))),
    "!foo,bar"  -> Right(Or(Not(Atom("foo")), Atom("bar"))),
    "!foo,!bar" -> Right(Or(Not(Atom("foo")), Not(Atom("bar")))),

    // Complex precedence: OR has lower precedence than AND
    "foo,bar baz" -> Right(
      Or(Atom("foo"), And(Atom("bar"), Atom("baz")))
    ),
    "foo,bar baz,qux" -> Right(
      Or(
        Or(Atom("foo"),
           And(Atom("bar"), Atom("baz"))),
        Atom("qux")
      )
    ),

    // Parentheses change precedence
    "(foo,bar) (baz,qux)" -> Right(
      And(
        Or(Atom("foo"), Atom("bar")),
        Or(Atom("baz"), Atom("qux"))
      )
    ),

    // Example: (x y)
    "(x y)" -> Right(And(Atom("x"), Atom("y"))),

    // Example: !(z,t)
    "!(z,t)" -> Right(Not(Or(Atom("z"), Atom("t")))),

    // Full example: foo,bar !baz,(x y),!(z,t)
    "foo,bar !baz,(x y),!(z,t)" -> Right(
      Or(
        Or(
          Or(
            Atom("foo"),
            And(Atom("bar"), Not(Atom("baz")))
          ),
          And(Atom("x"), Atom("y"))
        ),
        Not(Or(Atom("z"), Atom("t")))
      )
    ),

    // Wildcard patterns
    "bug*"      -> Right(Wildcard.unsafeFromPattern("bug*")),
    "*bug"      -> Right(Wildcard.unsafeFromPattern("*bug")),
    "bug?"      -> Right(Wildcard.unsafeFromPattern("bug?")),
    "?bug"      -> Right(Wildcard.unsafeFromPattern("?bug")),
    "first*bug" -> Right(Wildcard.unsafeFromPattern("first*bug")),
    "a?c"       -> Right(Wildcard.unsafeFromPattern("a?c")),

    // Wildcards with colon
    "test:*" -> Right(Wildcard.unsafeFromPattern("test:*")),
    "*:prod" -> Right(Wildcard.unsafeFromPattern("*:prod")),

    // Wildcards in combinations
    "bug*,feature*" -> Right(Or(Wildcard.unsafeFromPattern("bug*"),
                                Wildcard.unsafeFromPattern("feature*"))),
    "test* prod" -> Right(And(Wildcard.unsafeFromPattern("test*"),
                              Atom("prod"))),
    "!bug*" -> Right(Not(Wildcard.unsafeFromPattern("bug*")))
  ).map { case (expr, expected) =>
    pureTest(s"'$expr' should be parsed to $expected") {
      val result = TagExprParser.parse(expr)
      expect.same(expected, result)
    }
  }
}
