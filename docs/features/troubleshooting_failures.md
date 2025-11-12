# Troubleshooting failures

You can add diagnostic information to your tests by using clues and log statements.

You should add information when you write a test, and not just when it fails. This will save you needing to write more code when your test fails later.

This information will only be displayed when a test fails, so will not clutter your test report.

## Using `clue` with `expect`

The `expect` and `expect.all` statements are used to assert on boolean values. On their own, they don't print any diagnostic information.

You can add information by using the `clue` function.

```scala mdoc:invisible
import weaver._
import weaver.Expectations.Helpers._
```

```scala mdoc:silent
val x = List(1, 2, 3)

expect(clue(x.size) > 2)
```

You can add clues around any variable.

```scala mdoc:silent
expect(clue(x).size > 2)
```

You can also nest them.

```scala mdoc:silent
expect(clue(clue(x).size) > 2)
```

The clues are displayed when the test fails.

```scala mdoc
import weaver._

object ExpectWithClueTest extends FunSuite {

  test("This fails, and we can diagnose why") {
    val x = 1
    val y = 2
    expect(clue(x) > clue(y))
  }

  test("This fails, but we don't know why") {
    val x = 1
    val y = 2
    expect(x > y)
  }

  test("This succeeds so nothing is printed") {
    val x = 1
    val y = 0
    expect(clue(x) > clue(y))
  }
}
```

### Example report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ExpectWithClueTest))
```

## Logging information

You can log general information using a lazy logger. Log statements are only printed when a test fails and are displayed along with the test failure.

Access the logger by writing a `loggedTest`.

```scala mdoc
import weaver._

object LoggedTests extends SimpleIOSuite {

  case class Response(body: String, status: Int)

  loggedTest("This test fails") { log =>
    val response = Response("this went wrong", 500)
    for {
      _ <- log.info(s"The response body is: ${response.body}")
    } yield expect.eql(200, response.status)
  }

  loggedTest("This test also fails, and has separate logs") { log =>
    val response = Response("this also went wrong", 400)
    for {
      _ <- log.info(s"The response body is: ${response.body}")
    } yield expect.eql(200, response.status)
  }

  loggedTest("This test succeeds, so no logs are printed") { log =>
    val response = Response("OK", 200)
    for {
      _ <- log.info(s"The response body is: ${response.body}")
    } yield expect.eql(200, response.status)
  }
}
```

### Example report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(LoggedTests))
```

### Use `log.info` instead of `println`

You should use log statements instead of `println` statements.

Because weaver runs tests in parallel, it is difficult to pinpoint which test a `println` statement corresponds to.

Unlike log statements, `println` statements are always printed, regardless of whether a test succeeds or fails, so can clutter the test output.

Log statements are only printed if a test fails. The logs statements are displayed along with the test failure they correspond to, so it's easy to troubleshoot tests even though they are run in parallel.

### Accessing the logger along with shared resources

You can access the logger along with [shared resources](resources.md) by using the second argument of the `test` function.

```scala mdoc
import weaver._
import cats.effect._

import org.http4s.ember.client._
import org.http4s.client._

object HttpSuite extends IOSuite {

  override type Res = Client[IO]
  override def sharedResource : Resource[IO, Res] =
    EmberClientBuilder.default[IO].build

  // The log is passed as the second argument
  test("This test fails") { (httpClient, log) =>
    for {
      statusCode <- httpClient.get("https://httpbin.org/oops") {
        response => for {
          _ <- log.info(s"Content length: ${response.contentLength}")
        } yield response.status.code
      }
    } yield expect.eql(200, statusCode)
  }
}
```

### Example report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(HttpSuite))
```

## Displaying the correct source location

Assertions such as `expect` and `expect.eql` display their source location when they fail.

If you make assertions in helper functions, you might want to display the source location of the call to the helper.

You can do this by requiring an implicit `SourceLocation` argument.

```scala mdoc
import weaver._

object SourceLocationSuite extends FunSuite {

  case class Response(body: String, status: Int)

  test("This test fails") {
    val response = Response("this went wrong", 500)
    expectOk(response) // The failure points here
  }

  test("This test fails with a different source location") {
    val response = Response("this also went wrong", 500)
    expectOk(response) // The other failure points here
  }

  def expectOk(response: Response)(implicit loc: SourceLocation): Expectations = {
     expect.eql(200, response.status) && expect.eql("OK", response.body)
  }
}
```

### Example report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(SourceLocationSuite))
```

## Displaying a trace

If you have a test codebase with many nested helper functions, you may want to display a trace of source locations. You can do this with the `traced(here)` function.

In the following example, `expectOk` calls `expectStatus` and `expectOkBody`. When the test fails, the source location of both `expectStatus` and `expectOk` is displayed.

```scala mdoc
import weaver._

object TracingSuite extends FunSuite {

  case class Response(body: String, status: Int)

  test("This test fails") {
    val response = Response("this went wrong", 500)
    expectOk(response) // The failure points here first
  }

  def expectOk(response: Response)(implicit loc: SourceLocation): Expectations = {
     expectOkStatus(response)
       .traced(here) // The failure points here third
  }

  def expectOkStatus(response: Response)(implicit loc: SourceLocation): Expectations = {
     expect.eql(200, response.status)
       .traced(here) // The failure points here second
  }

}
```

### Example report

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(TracingSuite))
```
