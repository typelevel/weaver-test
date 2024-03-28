Ignoring tests
==============

If your want to (temporarily) ignore tests, you can use add `ignore` to your test description.

```scala mdoc
object CatsFunSuite extends weaver.FunSuite {
  test("fails".ignore) { expect(Some(25).contains(5)) }

  test("throws".ignore) { throw new RuntimeException("oops") }
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(CatsFunSuite))
```
