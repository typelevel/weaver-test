Expectations (assertions)
=========================

Expectations are pure, composable values. This forces developers to separate the test's checks from the scenario, which is generally cleaner/clearer.

The easiest way to construct expectactions is to call the `expect` macro. The `clue` function can be used to investigate failures.

## TL;DR

```scala mdoc:invisible
import weaver.Expectations.Helpers._
import cats.effect.IO
val list = List(1)
```

- Assert on boolean values using `expect`:

  ```scala mdoc:compile-only
  expect(list.size > 4)
  ```

- Investigate failures using `clue`:

   ```scala mdoc:compile-only
   expect(clue(list).size > 4)
   ```

- Compare values using `expect.eql` (needs an `Eq` instance)

  ```scala mdoc:compile-only
  expect.eql(List(1, 2, 3), (1 to 3).toList)
  ```

- Compare values with relaxed equality using `expect.same` (no `Eq` instance needed)

  ```scala mdoc:compile-only
  expect.same(List(1, 2, 3), (1 to 3).toList)
  ```

- Compose expectations using `and`/`or`

  ```scala mdoc:compile-only
  (expect(1 > 0) and expect(2 > 1)) or expect(5 < 6)
  ```

- ... or their symbolic aliases `&&`/`||`

  ```scala mdoc:compile-only
  (expect(1 > 0) && expect(2 > 1)) || expect(5 < 6)
  ```

- Compose expectations using `xor`

  ```scala mdoc:compile-only
  expect(1 > 0) xor expect(2 > 1)
  ```

- Use varargs short form for asserting on all boolean values

  ```scala mdoc:compile-only
  expect.all(1 > 0, 2 > 1, 3 > 2)
  ```

- Use `forEach` to test every element of a collection (or anything that
    implements `Foldable`)

  ```scala mdoc:compile-only
  forEach(List(1, 2, 3))(i => expect(i < 5))
  ```

- Use `exists` to assert that at least one element of collection matches
    expectations:

  ```scala mdoc:compile-only
  exists(Option(5))(n => expect(n > 3))
  ```

- Use `matches` to assert that an expression matches a given pattern

  ```scala mdoc:compile-only
  matches(Option(4)) { case Some(x) =>
    expect.eql(4, x)
  }
  ```

- Use `whenSuccess` to assert that a structure with an error channel (like `Either`) is successful

  ```scala mdoc:compile-only
  val res: Either[String, Int] =
    Right(4)
  
  whenSuccess(res) { n =>
    expect.eql(4, n)
  }
  ```

- Use `success` or `failure` to create succeeding/failing expectations without
    conditions

  ```scala mdoc:compile-only
  val result = if(5 == 5) success else failure("oh no")
  ```

- Use `.failFast` to evaluate the expectation eagerly and raise the assertion error in your effect type

  ```scala mdoc:compile-only
  for {
    x <- IO("hello")
    _ <- expect(x.length < 4).failFast[IO]
    y = x + "bla"
    _ <- expect(y.size > x.size).failFast[IO]
  } yield expect(y.contains(x))
  ```

- Similarly `matchOrFailFast` can be used assert an expression matches a given pattern, and return it if so

  ```scala mdoc:compile-only
  for {
    b <- IO(Some(4))
    s <- matchOrFailFast[IO](b) {
      case Some(v) => v.toString    
    }
    c <- IO("4")
  } yield expect.eql(s, c)
  ```

## Example suite 

```scala mdoc
import weaver._
import cats.effect.IO

object ExpectationsSuite extends SimpleIOSuite {

  def test(a: Int) = a + 5

  pureTest("Simple expectations (success)") {
    val z = 15

    expect(test(z) < z + 6)
  }

  pureTest("Simple expectations (failure)") {
    val z = 15

    expect(clue(test(z)) > z + 6)
  }

  pureTest("And/Or composition (success)") {
    expect(1 != 2) and expect(2 != 1) or expect(2 != 3)
  }

  pureTest("And/Or composition (failure)") {
    (expect(1 != clue(2)) and expect(2 < clue(1))) or expect(2 > clue(3))
  }

  pureTest("Varargs composition (success)") {
    // expect(1 + 1 < 3) && expect(2 + 2 < 5) && expect(4 * 2 < 9)
    expect.all(1 + 1 < 3, 2 + 2 < 5, 4 * 2 < 9)
  }

  pureTest("Varargs composition (failure)") {
    // expect(1 + 1 < 3) && expect(clue(2 + 2) > 5) && expect(4 * 2 < 9)
    expect.all(1 + 1 < 3, clue(2 + 2) > 5, 4 * 2 < 9)
  }

  pureTest("Working with collections (success)") {
    forEach(List(1, 2, 3))(i => expect(i < 5)) and
      forEach(Option("hello"))(msg => expect.eql(msg, "hello")) and
      exists(List("a", "b", "c"))(i => expect.eql(i, "c")) and
      exists(Vector(true, true, false))(i => expect.eql(i, false))
  }

  pureTest("Working with collections (failure 1)") {
    forEach(Vector("hello", "world"))(msg => expect.eql(msg, "hello"))
  }

  pureTest("Working with collections (failure 2)") {
    exists(Option(39))(i => expect(clue(i) > 50))
  }

  import cats.Eq
  case class Test(d: Double)

  implicit val eqTest: Eq[Test] = Eq.by[Test, Double](_.d)

  pureTest("Strict equality (success)") {
    expect.eql("hello", "hello") and
      expect.eql(List(1, 2, 3), List(1, 2, 3)) and
      expect.eql(Test(25.0), Test(25.0))
  }

  pureTest("Strict equality (failure 1)") {
    expect.eql("hello", "world")
  }

  pureTest("Strict equality (failure 2)") {
    expect.eql(List(1, 2, 3), List(1, 19, 3))
  }

  pureTest("Strict equality (failure 3)") {
    expect.eql(Test(25.0), Test(50.0))
  }

  // Note that we don't have an instance of Eq[Hello]
  // anywhere in scope
  class Hello(val d: Double) {
    override def toString = s"Hello to $d"

    override def equals(other: Any) =
      if(other != null && other.isInstanceOf[Hello])
        other.asInstanceOf[Hello].d == this.d
      else
        false
  }

  pureTest("Relaxed equality comparison (success)") {
    expect.same(new Hello(25.0), new Hello(25.0))
  }

  pureTest("Relaxed equality comparison (failure)") {
    expect.same(new Hello(25.0), new Hello(50.0))
  }

  pureTest("Non macro-based expectations") {
    val condition : Boolean = false
    if (condition) success else failure("Condition failed")
  }

  test("Failing fast expectations") {
    for {
      h <- IO.pure("hello")
      _ <- expect(clue(h).isEmpty).failFast
    } yield success
  }

  test("Failing fast match") {
    for {
      h <- IO.pure(Some(4))
      x <- matchOrFailFast[IO](h) {
        case Some(v) => v
      }
      g <- IO.pure(Option.empty[Int])
      y <- matchOrFailFast[IO](g) {
        case Some(v) => v
      }
    } yield expect.eql(x, y)
  }
}
```

## Example suite report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ExpectationsSuite))
```
