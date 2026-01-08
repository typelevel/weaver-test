# Defining custom test functions

Weaver's `IOSuite` comes with a `test` function that provides logging, shared resource management and more. You can extend this by defining your own test functions.

## Defining additional test functions

You can define additional test functions by calling `registerTest`.

For example, you can define a `timedTest` function to explicitly print the test duration to the log.

```scala mdoc
import weaver._
import cats.effect._

trait TimedSuite extends SimpleIOSuite {

  def timedTest(name: TestName)(run: IO[Expectations]): Unit =
    registerTest(name)(_ =>
      Test[IO](
        name.name,
        (log: Log[IO]) =>
          // Run the test, then log the time.
          for {
            (time, expectations) <- run.timed
            _                    <- log.info(s"Test took ${time.toSeconds} seconds")
          } yield expectations
      )
    )
}
```

The `timedTest` function behaves like any other `test` function.

```scala mdoc
import scala.concurrent.duration.*

object ExampleSuite extends TimedSuite {
  timedTest("timed test")(IO.sleep(2.seconds).as(failure("failed")))
}
```

### Example report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ExampleSuite))
```

## Redefining the `test` function

You cannot redefine weaver's `test` function, but you can define your own by extending `SharedResourceSuite` and implementing `test`.

For example, to time tests defined with your own `test` function:

```scala mdoc:reset
import weaver._
import cats.effect._

trait TimedSuite extends SharedResourceSuite[IO] with BaseIOSuite with Expectations.Helpers {

  // Define the shared resource type
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure[IO, Unit](())

  // Define the test function
  def test(name: TestName)(run: IO[Expectations]): Unit =
    registerTest(name)(_ =>
      Test[IO](
        name.name,
        (log: Log[IO]) =>
          for {
            (time, expectations) <- run.timed
            _                    <- log.info(s"Test took ${time.toSeconds} seconds")
          } yield expectations
      )
    )
}
```

This can be used in the same way as weaver's `test` function.

```scala mdoc
import scala.concurrent.duration._

object ExampleSuite extends TimedSuite {
  test("timed test")(IO.sleep(2.seconds).as(failure("failed")))
}

```

### Example report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ExampleSuite))
```
