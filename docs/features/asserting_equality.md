# Asserting equality

```scala mdoc:invisible
import weaver.Expectations.Helpers._
val expected = 1
val found = 2
```

Weaver offers two approaches for asserting equality: `expect.eql` and `expect.same`.

`expect.eql` is the better approach, but you may need to write some extra code to use it.

## `expect.eql` (recommended)

```scala mdoc:compile-only
expect.eql(expected, found)
```

`expect.eql` asserts for strict equality.

If you accidentally compare the wrong types of data, your code will not compile:

```scala mdoc:fail
expect.eql(1, "one")
```

The data types you compare must have an [Eq](https://typelevel.org/cats/typeclasses/eq.html) typeclass instance. You can declare one, or derive one using [kittens](https://github.com/typelevel/kittens).

```scala mdoc:compile-only
case class Pet(name: String)

// A cats.Eq instance is needed
import cats.Eq
implicit val petEq: Eq[Pet] = Eq.by(_.name)

expect.eql(Pet("Maru"), Pet("Fido"))
```

## `expect.same`

```scala mdoc:compile-only
expect.same(expected, found)
```

`expect.same` asserts using universal equality. 

If you accidentally compare the wrong types of data, your code will compile. Your test will fail later, when it is run:

```scala
// This compiles
expect.same(1, "one")
```


`expect.same` doesn't require an `Eq` instance. 

```scala mdoc:compile-only
case class Dog(name: String) // No Eq instance defined

expect.same(Dog("Maru"), Dog("Fido"))
```

## Do not use `expect`

You can assert for equality using `expect`.

```scala mdoc:compile-only
expect(1 == 2)
```

This is discouraged because its failure messages are poorer than `expect.eql` or `expect.same`. It does not display a diff of the values on the left and right side of the equality.

Use `expect` along with `clue` when you have other assertions. For example to assert for `>`

```scala mdoc:invisible
val x = 1
```

```scala mdoc:compile-only
expect(clue(x) > 2)
```

## Should I use `expect.same` or `expect.eql`?

- Use `expect.eql` for Scala standard datatypes such as `Int` and `String`. These have `Eq` instances, so don't need any extra code.
- Use `expect.eql` for your own data types if you prefer compile time errors at the expense of extra code.
- Use `expect.same` for your own data types if you prefer runtime test failures instead of extra code.

These are described in the suite below.

### Example suite

```scala mdoc
import weaver._

object ExpectationsSuite extends SimpleIOSuite {

  pureTest("expect.eql for standard data types") {
    expect.eql(1, 2)
  }
  
  import cats.Eq
  case class Pet(name: String)
  implicit val eqPet: Eq[Pet] = Eq.by[Pet, String](_.name)
  
  pureTest("expect.eql for user-defined data types") {
    expect.same(Pet("Maru"), Pet("Fido"))
  }

  // Note that we don't have an instance of Eq[Dog]
  // anywhere in scope
  case class Dog(name: String)

  pureTest("expect.same relaxed equality comparison") {
    expect.same(Dog("Maru"), Dog("Fido"))
  }
}
```

### Example suite report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ExpectationsSuite))
```
