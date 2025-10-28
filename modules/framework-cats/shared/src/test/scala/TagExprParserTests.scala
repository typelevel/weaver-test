package weaver
package framework
package test

import weaver.internals.TagExprParser
import weaver.internals.TagExpr.*

object TagExprParserTests extends SimpleIOSuite {

  List(
    // Basic atom
    "foo"   -> Right(Wildcard.unsafeFromPattern("foo")),
    "(foo)" -> Right(Wildcard.unsafeFromPattern("foo")),

    // NOT expressions
    "!foo"   -> Right(Not(Wildcard.unsafeFromPattern("foo"))),
    "!(foo)" -> Right(Not(Wildcard.unsafeFromPattern("foo"))),
    "!!foo"  -> Right(Not(Not(Wildcard.unsafeFromPattern("foo")))),

    // OR expressions
    "foo,bar" -> Right(Or(Wildcard.unsafeFromPattern("foo"),
                          Wildcard.unsafeFromPattern("bar"))),

    // AND expressions
    "foo bar" -> Right(And(Wildcard.unsafeFromPattern("foo"),
                           Wildcard.unsafeFromPattern("bar"))),

    // Combined expressions
    "foo,!bar" -> Right(Or(Wildcard.unsafeFromPattern("foo"),
                           Not(Wildcard.unsafeFromPattern("bar")))),
    "!foo,bar" -> Right(Or(Not(Wildcard.unsafeFromPattern("foo")),
                           Wildcard.unsafeFromPattern("bar"))),
    "!foo,!bar" -> Right(Or(Not(Wildcard.unsafeFromPattern("foo")),
                            Not(Wildcard.unsafeFromPattern("bar")))),

    // Complex precedence: OR has lower precedence than AND
    "foo,bar baz" -> Right(
      Or(Wildcard.unsafeFromPattern("foo"),
         And(Wildcard.unsafeFromPattern("bar"),
             Wildcard.unsafeFromPattern("baz")))
    ),
    "foo,bar baz,qux" -> Right(
      Or(
        Or(Wildcard.unsafeFromPattern("foo"),
           And(Wildcard.unsafeFromPattern("bar"),
               Wildcard.unsafeFromPattern("baz"))),
        Wildcard.unsafeFromPattern("qux")
      )
    ),

    // Parentheses change precedence
    "(foo,bar) (baz,qux)" -> Right(
      And(
        Or(Wildcard.unsafeFromPattern("foo"),
           Wildcard.unsafeFromPattern("bar")),
        Or(Wildcard.unsafeFromPattern("baz"), Wildcard.unsafeFromPattern("qux"))
      )
    ),

    // Example: (x y)
    "(x y)" -> Right(And(Wildcard.unsafeFromPattern("x"),
                         Wildcard.unsafeFromPattern("y"))),

    // Example: !(z,t)
    "!(z,t)" -> Right(Not(Or(Wildcard.unsafeFromPattern("z"),
                             Wildcard.unsafeFromPattern("t")))),

    // Full example: foo,bar !baz,(x y),!(z,t)
    "foo,bar !baz,(x y),!(z,t)" -> Right(
      Or(
        Or(
          Or(
            Wildcard.unsafeFromPattern("foo"),
            And(Wildcard.unsafeFromPattern("bar"),
                Not(Wildcard.unsafeFromPattern("baz")))
          ),
          And(Wildcard.unsafeFromPattern("x"), Wildcard.unsafeFromPattern("y"))
        ),
        Not(Or(Wildcard.unsafeFromPattern("z"),
               Wildcard.unsafeFromPattern("t")))
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
                              Wildcard.unsafeFromPattern("prod"))),
    "!bug*" -> Right(Not(Wildcard.unsafeFromPattern("bug*")))
  ).map { case (expr, expected) =>
    pureTest(s"'$expr' should be parsed to $expected") {
      val result = TagExprParser.parse(expr)
      expect.same(expected, result)
    }
  }
}
