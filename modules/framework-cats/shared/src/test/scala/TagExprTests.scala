package weaver
package framework
package test

import weaver.internals.TagExpr.*

object TagExprTests extends SimpleIOSuite {

  pureTest("Atom matches exact tag") {
    val expr = Atom("foo")
    val tags = Set("foo", "bar", "baz")
    expect(expr.eval(tags))
  }

  pureTest("Atom does not match different tag") {
    val expr = Atom("foo")
    val tags = Set("bar", "baz")
    expect(!expr.eval(tags))
  }

  // Wildcard pattern parsing tests
  pureTest("Wildcard.fromPattern should parse literal pattern") {
    val result = Wildcard.fromPattern("foo")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with star") {
    val result = Wildcard.fromPattern("bug*")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with leading star") {
    val result = Wildcard.fromPattern("*bug")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with question mark") {
    val result = Wildcard.fromPattern("bug?")
    expect(clue(result).isRight)
  }

  pureTest(
    "Wildcard.fromPattern should parse pattern with leading question mark") {
    val result = Wildcard.fromPattern("?bug")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with star in middle") {
    val result = Wildcard.fromPattern("first*bug")
    expect(clue(result).isRight)
  }

  pureTest(
    "Wildcard.fromPattern should parse pattern with question mark in middle") {
    val result = Wildcard.fromPattern("a?c")
    expect(clue(result).isRight)
  }

  pureTest(
    "Wildcard.fromPattern should parse pattern with multiple wildcards") {
    val result = Wildcard.fromPattern("a*b*c")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with colon") {
    val result = Wildcard.fromPattern("test:unit")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with colon and star") {
    val result = Wildcard.fromPattern("test:*")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with hyphen") {
    val result = Wildcard.fromPattern("bug-123")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with underscore") {
    val result = Wildcard.fromPattern("test_case")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse pattern with only star") {
    val result = Wildcard.fromPattern("*")
    expect(clue(result).isRight)
  }

  pureTest(
    "Wildcard.fromPattern should parse pattern with only question mark") {
    val result = Wildcard.fromPattern("?")
    expect(clue(result).isRight)
  }

  pureTest("Wildcard.fromPattern should parse complex pattern") {
    val result = Wildcard.fromPattern("test-*:prod-?")
    expect(clue(result).isRight)
  }

  // Negative tests: invalid patterns
  pureTest("Wildcard.fromPattern should reject empty pattern") {
    val result = Wildcard.fromPattern("")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with spaces") {
    val result = Wildcard.fromPattern("foo bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with comma") {
    val result = Wildcard.fromPattern("foo,bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with exclamation mark") {
    val result = Wildcard.fromPattern("!foo")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with parentheses") {
    val result = Wildcard.fromPattern("(foo)")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with at sign") {
    val result = Wildcard.fromPattern("foo@bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with hash") {
    val result = Wildcard.fromPattern("foo#bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with dollar sign") {
    val result = Wildcard.fromPattern("foo$bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with percent") {
    val result = Wildcard.fromPattern("foo%bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with ampersand") {
    val result = Wildcard.fromPattern("foo&bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with slash") {
    val result = Wildcard.fromPattern("foo/bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with backslash") {
    val result = Wildcard.fromPattern("foo\\bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with brackets") {
    val result = Wildcard.fromPattern("foo[bar]")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with pipe") {
    val result = Wildcard.fromPattern("foo|bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with semicolon") {
    val result = Wildcard.fromPattern("foo;bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with dot") {
    val result = Wildcard.fromPattern("foo.bar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with tab") {
    val result = Wildcard.fromPattern("foo\tbar")
    expect(clue(result).isLeft)
  }

  pureTest("Wildcard.fromPattern should reject pattern with newline") {
    val result = Wildcard.fromPattern("foo\nbar")
    expect(clue(result).isLeft)
  }

  // Wildcard matching tests
  pureTest("Wildcard: bug* matches bug") {
    whenSuccess(Wildcard.fromPattern("bug*")) { wildcard =>
      val tags = Set("bug")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: bug* matches bug-123") {
    whenSuccess(Wildcard.fromPattern("bug*")) { wildcard =>
      val tags = Set("bug-123")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: bug* matches bugfix") {
    whenSuccess(Wildcard.fromPattern("bug*")) { wildcard =>
      val tags = Set("bugfix")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: bug* does not match foo") {
    whenSuccess(Wildcard.fromPattern("bug*")) { wildcard =>
      val tags = Set("foo")
      expect(!clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: *bug matches bug") {
    whenSuccess(Wildcard.fromPattern("*bug")) { wildcard =>
      val tags = Set("bug")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: *bug matches critical-bug") {
    whenSuccess(Wildcard.fromPattern("*bug")) { wildcard =>
      val tags = Set("critical-bug")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: *bug does not match bugfix") {
    whenSuccess(Wildcard.fromPattern("*bug")) { wildcard =>
      val tags = Set("bugfix")
      expect(!clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: bug? matches bug1") {
    whenSuccess(Wildcard.fromPattern("bug?")) { wildcard =>
      val tags = Set("bug1")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: bug? matches buga") {
    whenSuccess(Wildcard.fromPattern("bug?")) { wildcard =>
      val tags = Set("buga")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: bug? does not match bug") {
    whenSuccess(Wildcard.fromPattern("bug?")) { wildcard =>
      val tags = Set("bug")
      expect(!clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: bug? does not match bug12") {
    whenSuccess(Wildcard.fromPattern("bug?")) { wildcard =>
      val tags = Set("bug12")
      expect(!clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: ?bug matches abug") {
    whenSuccess(Wildcard.fromPattern("?bug")) { wildcard =>
      val tags = Set("abug")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: ?bug does not match bug") {
    whenSuccess(Wildcard.fromPattern("?bug")) { wildcard =>
      val tags = Set("bug")
      expect(!clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: first*bug matches firstbug") {
    whenSuccess(Wildcard.fromPattern("first*bug")) { wildcard =>
      val tags = Set("firstbug")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: first*bug matches first-critical-bug") {
    whenSuccess(Wildcard.fromPattern("first*bug")) { wildcard =>
      val tags = Set("first-critical-bug")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: first*bug does not match firstbugfix") {
    whenSuccess(Wildcard.fromPattern("first*bug")) { wildcard =>
      val tags = Set("firstbugfix")
      expect(!clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: a?c matches abc") {
    whenSuccess(Wildcard.fromPattern("a?c")) { wildcard =>
      val tags = Set("abc")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: a?c matches a1c") {
    whenSuccess(Wildcard.fromPattern("a?c")) { wildcard =>
      val tags = Set("a1c")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: a?c does not match ac") {
    whenSuccess(Wildcard.fromPattern("a?c")) { wildcard =>
      val tags = Set("ac")
      expect(!clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: a?c does not match abcd") {
    whenSuccess(Wildcard.fromPattern("a?c")) { wildcard =>
      val tags = Set("abcd")
      expect(!clue(wildcard).eval(tags))
    }
  }

  // Wildcards with colon
  pureTest("Wildcard: test:* matches test:unit") {
    whenSuccess(Wildcard.fromPattern("test:*")) { wildcard =>
      val tags = Set("test:unit")
      expect(clue(wildcard).eval(tags))
    }
  }

  pureTest("Wildcard: *:prod matches env:prod") {
    whenSuccess(Wildcard.fromPattern("*:prod")) { wildcard =>
      val tags = Set("env:prod")
      expect(clue(wildcard).eval(tags))
    }
  }

  // No wildcards (should work like exact match)
  pureTest("Wildcard: foo matches foo exactly") {
    whenSuccess(Wildcard.fromPattern("foo")) { wildcard =>
      val tags = Set("foo", "foobar")
      expect(clue(wildcard).eval(tags))
    }
  }
}
