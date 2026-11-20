ScalaCheck integration
======================

Weaver comes with basic [ScalaCheck](https://www.scalacheck.org/) integration, allowing for property-based testing.

## Installation

You'll need to install an additional dependency in order to use ScalaCheck with Weaver.

### SBT
```scala
libraryDependencies +=  "org.typelevel" %% "weaver-scalacheck" % "@VERSION@" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"org.typelevel::weaver-scalacheck:@VERSION@"
  )
}
```

## Usage

Add the `weaver.scalacheck.Checkers` mixin to use ScalaCheck within your test suite.

```scala mdoc:silent
import org.scalacheck.Gen

import weaver._
import weaver.scalacheck._

object ForallExamples extends SimpleIOSuite with Checkers {

  // CheckConfig can be overridden at the suite level
  override def checkConfig: CheckConfig =
    super.checkConfig.copy(perPropertyParallelism = 100)

  // Using a single `Gen` instance
  test("Single Gen form") {
    // Takes a single, explicit `Gen` instance
    forall(Gen.posNum[Int]) { a =>
      expect(a > 0)
    }
  }

  // There is only one overload for the `forall` that takes an explicit `Gen` parameter
  // To use multiple `Gen` instances, compose them monadically before passing to `forall`
  test("Multiple Gen form") {
    // Compose into a single `Gen[(Int, Int)]`
    val gen = for {
      a <- Gen.posNum[Int]
      b <- Gen.posNum[Int]
    } yield (a, b)

    // Unapply the tuple to access individual members
    forall(gen) { case (a, b) =>
      expect(a > 0) and expect(b > 0)
    }
  }

  // Using a number of `Arbitrary` instances
  test("Arbitrary form") {
    // There are 6 overloads, to pass 1-6 parameters
    forall { (a1: Int, a2: Int, a3: Int) =>
      expect(a1 * a2 * a3 == a3 * a2 * a1)
    }
  }
  
  test("Failure example") {
    // There are 6 overloads, to pass 1-6 parameters
    forall { (a1: Int, a2: Int) =>
      expect(a1 + a2 % 2 == 0)
    }
  }

  test("CheckConfig at test level") {
    // CheckConfig can be overridden locally at the test level
    forall.withConfig(CheckConfig.default.copy(perPropertyParallelism = 1)) {
      (x: Int) => expect(x > 0)
    }
  }

}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ForallExamples))
```

## Effectful properties

The body of a `forall` can also return an effect, such as `IO[Expectations]`, instead of a plain `Expectations`.
This is useful when the property needs to run a program, for example one that uses a `Ref` or other effectful resources.

Each generated input is evaluated as a separate effect.
The number of inputs evaluated concurrently is controlled by `perPropertyParallelism`, which uses the effect system's own concurrency rather than ScalaCheck's.
Set it to `1` if the property must evaluate its inputs sequentially.

```scala mdoc:silent
import cats.effect.{IO, Ref}

object EffectfulForallExamples extends SimpleIOSuite with Checkers {

  test("Effectful property") {
    forall(Gen.posNum[Int]) { n =>
      for {
        ref    <- Ref.of[IO, Int](0)
        _      <- ref.update(_ + n)
        result <- ref.get
      } yield expect.same(n, result)
    }
  }
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(EffectfulForallExamples))
```
