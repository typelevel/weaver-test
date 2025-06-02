Installation
============

@:callout(info)
The stewardship of Weaver has moved from `disneystreaming` to `typelevel`.

You can migrate from previous versions of weaver by following the [migration guide](https://github.com/typelevel/weaver-test/releases/tag/v0.9.0), or [learn more about the stewardship](../faqs/typelevel-stewardship.md).
@:@

You'll need to install the following dependencies to test your programs against `cats.effect.IO`

### SBT (1.9.0+)

Newer versions of SBT have [`weaver` automatically integrated](https://github.com/sbt/sbt/pull/7263).

```scala
libraryDependencies +=  "org.typelevel" %% "weaver-cats" % "@VERSION@" % Test
```

### SBT (older versions)

Internally, SBT has a hardcoded list of test frameworks it integrates with. `weaver` must be manually added to this list.

```scala
libraryDependencies +=  "org.typelevel" %% "weaver-cats" % "@VERSION@" % Test
testFrameworks += new TestFramework("weaver.framework.CatsEffect")
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"org.typelevel::weaver-cats:@VERSION@"
  )
  def testFramework = "weaver.framework.CatsEffect"
}
```

### scala-cli
```scala
//> using lib "org.typelevel::weaver-cats:@VERSION@"
//> using testFramework "weaver.framework.CatsEffect" // this may neccessary if you have other TestFramework on your dependencies
```

## Usage


Start with importing the following :

```scala mdoc
import weaver._
```

The most basic usage is to extend `SimpleIOSuite`. Tests are registered imperatively, very much like in scalatest's `FunSuite` or in `utest`, but their bodies are "weaved" together in a single `IO` that the framework executes when the build tool asks for it.


```scala mdoc
import cats.effect._

// Suites must be "objects" for them to be picked by the framework
object MySuite extends SimpleIOSuite {

  val randomUUID = IO(java.util.UUID.randomUUID())

  // A test for side-effecting functions
  test("hello side-effects") {
    for {
      x <- randomUUID
      y <- randomUUID
    } yield expect(x != y)
  }

}
```
