Tagging
=======

Weaver provides constructs to tag tests for organization and selective execution. Tags can be used to categorize tests (e.g., by type, priority, environment) and filter which tests run.

## Adding tags to tests

Tests can be tagged using the `.tagged()` method on the test name:

```scala mdoc
import weaver._

object TaggedSuite extends SimpleIOSuite {

  // Test with a single tag
  test("bug fix".tagged("bug")) {
    expect(1 + 1 == 2)
  }

  // Test with multiple tags (chain .tagged() calls)
  test("integration test".tagged("integration").tagged("slow").tagged("database")) {
    expect(true)
  }

  // Test without tags
  test("simple unit test") {
    expect(42 == 42)
  }
}
```

## Special tags: ignore and only

Weaver provides convenient shortcuts for two special tags:

### ignore

The `.ignore` method is equivalent to `.tagged("ignore")`:

```scala mdoc:reset
import weaver._

object IgnoreSuite extends SimpleIOSuite {

  // These two are equivalent
  test("skipped test".ignore) {
    expect(1 + 1 == 2)
  }

  test("also skipped".tagged("ignore")) {
    expect(1 + 1 == 2)
  }
}
```

### only

The `.only` method is equivalent to `.tagged("only")`:

```scala mdoc:reset
import weaver._

object OnlySuite extends SimpleIOSuite {

  // These two are equivalent
  test("run only this".only) {
    expect(1 + 1 == 2)
  }

  test("run only this too".tagged("only")) {
    expect(1 + 1 == 2)
  }

  test("this will be skipped") {
    expect(42 == 42)
  }
}
```

When any test in a suite is tagged with `only`, only those tests will run.

## Tag naming conventions

Tags should consist of alphanumeric characters, hyphens, underscores, and colons:

- Valid characters: `a-z`, `A-Z`, `0-9`, `_`, `-`, `:`
- Examples: `bug`, `bug-123`, `test_case`, `env:prod`

Common tagging strategies:

```scala
// By category
test("test name".tagged("unit"))
test("test name".tagged("integration"))
test("test name".tagged("e2e"))

// By priority
test("test name".tagged("critical"))
test("test name".tagged("high"))
test("test name".tagged("low"))

// By feature or issue
test("test name".tagged("bug-123"))
test("test name".tagged("feature-auth"))
test("test name".tagged("epic-billing"))

// By environment
test("test name".tagged("env:dev"))
test("test name".tagged("env:staging"))
test("test name".tagged("env:prod"))

// By speed
test("test name".tagged("fast"))
test("test name".tagged("slow"))

// By stability
test("test name".tagged("flaky"))
test("test name".tagged("stable"))

// Multiple tags
test("test name"
  .tagged("integration")
  .tagged("slow")
  .tagged("database")
  .tagged("env:staging"))
```

## Filtering tests by tags

See [Filtering](filtering.md) for complete documentation on how to filter tests using tag expressions.

Quick examples:

```
// Run only tests tagged as "bug"
> testOnly -- -t bug

// Run tests tagged as "bug" OR "feature"
> testOnly -- -t "bug,feature"

// Run tests tagged as both "integration" AND "database"
> testOnly -- -t "integration database"

// Run all tests except "slow" ones
> testOnly -- -t "!slow"

// Run bug tests with wildcards
> testOnly -- -t "bug-*"

// Exclude ignored tests explicitly
> testOnly -- -t "!ignore"
```

## Dynamic tagging (conditional ignore)

Weaver also provides constructs to dynamically tag tests as ignored at runtime:

```scala mdoc:reset
import weaver._
import cats.effect.IO
import cats.syntax.all._

object DynamicTaggingSuite extends SimpleIOSuite {

  test("Only on CI") {
    for {
      onCI <- IO(sys.env.get("CI").isDefined)
      _    <- ignore("not on CI").unlessA(onCI)
      x    <- IO.delay(1)
      y    <- IO.delay(2)
    } yield expect(x == y)
  }
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(DynamicTaggingSuite))
```

This allows you to conditionally skip tests based on runtime conditions like environment variables, system properties, or other dynamic checks.
