Filtering tests
===============

## Running a subset of tests

You can run a set of tests with `sbt` using the `testOnly` command:

``` 
> testOnly -- -o *foo*
```

This filter will prevent the execution of any test that doesn't contain the string "foo" in is qualified name. For a test labeled "foo" in a "FooSuite" object, in the package "fooPackage", the qualified name of a test is:

```
fooPackage.FooSuite.foo
```

## Running a single test

You can run a single test in a suite by appending `only` to the test name:

```scala mdoc
import weaver._

object ExampleSuite extends FunSuite {

  test("Only run this test".only) {
    expect.eql(1, 2)
  }
  
  test("This test will not be run") {
      expect.eql(1, 1)
  }
}
```

## Ignoring tests

You can mark a test as ignored by appending `ignore` to the test name. The test will not be run as part of your test suite.

```scala mdoc
import weaver._

object IgnoreSuite extends FunSuite {

  test("This test is not run".ignore) {
    expect.eql(1, 2)
  }
  test("This test is run") {
    expect.eql(1, 1)
  }
}
```

### Example report

```scala mdoc:passthrough 
println(weaver.docs.Output.runSuites(IgnoreSuite))
```

## Dynamically ignoring tests

You can also dynamically mark tests as ignored using the `ignore` function:

```scala mdoc
import weaver._
import cats.effect.IO
import cats.syntax.all._

object DynamicIgnoreSuite extends SimpleIOSuite {

  test("Only on CI") {
    for {
      onCI <- IO(sys.env.get("CI").isDefined)
      _    <- ignore("not on CI").unlessA(onCI)
    } yield expect.eql(1, 2)
  }
}
```

### Example report

```scala mdoc:passthrough 
println(weaver.docs.Output.runSuites(DynamicIgnoreSuite))
```
