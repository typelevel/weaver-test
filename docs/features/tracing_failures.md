Tracing Failures
================

# Tracing locations of failed expectations

As of 0.5.0, failed expectations carry a `NonEmptyList[SourceLocation]`, which can be used to manually trace the callsites that lead to a failure.

By default, the very location where the expectation is created is captured, but the `traced` method can be use to add additional locations to the expectation.


```scala mdoc
import weaver._

object TracingSuite extends SimpleIOSuite {

  pureTest("Tracing example") {
    foo
  }

  def foo(implicit loc : SourceLocation) = bar().traced(loc).traced(here)

  def bar() = baz().traced(here)

  def baz() = expect(1 != 1)
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(TracingSuite))
```
