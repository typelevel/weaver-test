package weaver
package framework
package test

import cats.data.Chain
import cats.effect.{ IO, Resource }
import cats.syntax.all._

import sbt.testing.Status.Error
import snapshot4s.generated.snapshotConfig
import SnapshotExpectations.*

object DogFoodTests extends IOSuite {

  type Res = DogFood[IO]
  def sharedResource: Resource[IO, DogFood[IO]] =
    DogFood.make(new CatsEffect)

  test("test suite reports successes events") { dogfood =>
    import dogfood._
    runSuite(Meta.MutableSuiteTest).map {
      case (_, events) => forEach(events)(isSuccess)
    }
  }

  test(
    "the framework reports exceptions occurring during suite initialisation") {
    _.runSuite("weaver.framework.test.Meta$CrashingSuite").map {
      case (logs, events) =>
        val errorLogs = extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }

        exists(events.headOption) { event =>
          val name = event.fullyQualifiedName()
          expect.all(
            name == "weaver.framework.test.Meta$CrashingSuite",
            event.status() == Error
          )
        } and exists(errorLogs) { log =>
          expect.all(
            log.contains("Unexpected failure"),
            log.contains("Boom")
          )
        }
    }
  }

  test(
    "test suite outputs failed test names alongside successes in status report") {
    _.runSuite(Meta.FailingTestStatusReporting).flatMap {
      case (logs, _) =>
        val statusReport = outputBeforeFailures(logs).mkString_("\n").trim()

        assertInlineSnapshot(
          statusReport,
          """weaver.framework.test.Meta$FailingTestStatusReporting
+ I succeeded 0ms
- I failed 0ms
+ I succeeded again 0ms"""
        )
    }
  }

  test("test suite outputs logs for failed tests") {
    _.runSuite(Meta.FailingSuiteWithLogs).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(failure)")
        assertInlineSnapshot(
          actual,
          """- (failure) 0ms
  expected (src/main/DogFoodTests.scala:5)

    [INFO]  12:54:35 [DogFoodTests.scala:5] this test
    [ERROR] 12:54:35 [DogFoodTests.scala:5] has failed
    [DEBUG] 12:54:35 [DogFoodTests.scala:5] with context
        a       -> b
        token   -> <something>
        request -> true"""
        )
    }
  }

  test("test suite renders logs for tests with multiple failures") {
    _.runSuite(Meta.FailingSuiteWithLogs).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(multiple-failures)")
        assertInlineSnapshot(
          actual,
          """- (multiple-failures) 0ms
 [0] expected (src/main/DogFoodTests.scala:5)

 [1] another (src/main/DogFoodTests.scala:5)

    [INFO]  12:54:35 [DogFoodTests.scala:5] this test
    [ERROR] 12:54:35 [DogFoodTests.scala:5] has failed
    [DEBUG] 12:54:35 [DogFoodTests.scala:5] with context
        a       -> b
        token   -> <something>
        request -> true"""
        )
    }
  }

  // https://github.com/disneystreaming/weaver-test/issues/724
  test("test suite outputs stack traces even if the output is very long") {
    _.runSuite(Meta.ErroringWithLongPayload).map {
      case (logs, _) =>
        val actual = extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

        val expected =
          s"""
            |- erroring with a long message: ${Meta.ErroringWithLongPayload.smiles} 0ms
            |  Meta$$CustomException: surfaced error
            |
            |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
            |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$$new$$1
            |  <snipped>                cats.effect.internals.<...>
            |  <snipped>                java.util.concurrent.<...>
            |""".stripMargin.trim

        expect.same(actual, expected)
    }
  }

  test("test suite outputs stack traces of exception causes") {
    _.runSuite(Meta.ErroringWithCauses).flatMap {
      case (logs, _) =>
        val actual = extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

        assertInlineSnapshot(
          actual,
          """- erroring with causes 0ms
  Meta$CustomException: surfaced error

  DogFoodTests.scala:15    my.package.MyClass#MyMethod
  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1

  Caused by: weaver.framework.test.Meta$CustomException: first cause

  DogFoodTests.scala:15    my.package.MyClass#MyMethod
  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
  <snipped>                cats.effect.internals.<...>
  <snipped>                java.util.concurrent.<...>

  Caused by: weaver.framework.test.Meta$CustomException: root

  DogFoodTests.scala:15    my.package.MyClass#MyMethod
  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
  <snipped>                cats.effect.internals.<...>
  <snipped>                java.util.concurrent.<...>"""
        )
    }
  }

  test("failures with exceptions in logs display them correctly") {
    _.runSuite(Meta.SucceedsWithErrorInLogs).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(failure)")
        assertInlineSnapshot(
          actual,
          """- (failure) 0ms
  expected (src/main/DogFoodTests.scala:5)

    [ERROR] 12:54:35 [DogFoodTests.scala:5] error
    weaver.framework.test.Meta$CustomException: surfaced error
        DogFoodTests.scala:15    my.package.MyClass#MyMethod
        DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
        <snipped>                cats.effect.internals.<...>
        <snipped>                java.util.concurrent.<...>"""
        )
    }
  }

  test("failures with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual = extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

        assertInlineSnapshot(
          actual,
          """- lots 0ms
  of
  multiline
  (failure)
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(clue(x) == y)

  Clues {
    x: Int = 1
  }"""
        )
    }
  }

  test("successes with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual =
          extractLogEventBeforeFailures(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(success)") => msg
          }.get

        assertInlineSnapshot(actual,
                             """+ lots 0ms
  of
  multiline
  (success)""")
    }
  }

  test("ignored tests with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual =
          extractLogEventBeforeFailures(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(ignored)") => msg
          }.get
        assertInlineSnapshot(actual,
                             """- lots 0ms
  of
  multiline
  (ignored) !!! IGNORED !!!
  Ignore me (src/main/DogFoodTests.scala:5)""")
    }
  }

  test(
    "cancelled tests with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual =
          extractLogEventBeforeFailures(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(cancelled)") => msg
          }.get
        assertInlineSnapshot(actual,
                             """- lots 0ms
  of
  multiline
  (cancelled) !!! CANCELLED !!!
  I was cancelled :( (src/main/DogFoodTests.scala:5)""")
    }
  }

  test(
    "expect.eql delegates to Comparison show when an instance is found") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(eql Comparison)")
        assertInlineSnapshot(
          actual,
          """- (eql Comparison) 0ms
  Values not equal: (src/main/DogFoodTests.scala:5)

  => Diff (- obtained, + expected)
     s: foo
  -  i: 2
  +  i: 1
   }"""
        )
    }
  }

  test(
    "expect.same delegates to Comparison show when an instance is found") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(same Comparison)")
        assertInlineSnapshot(
          actual,
          """- (same Comparison) 0ms
  Values not equal: (src/main/DogFoodTests.scala:5)

  => Diff (- obtained, + expected)
     s: foo
  -  i: 2
  +  i: 1
   }"""
        )
    }
  }

  test("expect.eql values with the same string representation are rendered") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(eql Show)")
        assertInlineSnapshot(
          actual,
          """- (eql Show) 0ms
  Values not equal: (src/main/DogFoodTests.scala:5)

  Values have the same string representation. Consider modifying their Show instance.
  foo"""
        )
    }
  }

  test("expect statements with interpolators are rendered without warnings") {
    _.runSuite(Meta.Rendering).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(interpolator)")

        assertInlineSnapshot(
          actual,
          """- (interpolator) 0ms
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(s"$x" == "2")

  Use the `clue` function to troubleshoot"""
        )
    }
  }

  test("successes with clues are rendered correctly") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractLogEventBeforeFailures(logs) {
          case LoggedEvent.Info(msg) if msg.contains("(success)") =>
            msg
        }.get
        assertInlineSnapshot(actual, "+ (success) 0ms")
    }
  }

  test("failures with clues are rendered correctly") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(failure)")
        assertInlineSnapshot(
          actual,
          """- (failure) 0ms
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(clue(x) == clue(y))

  Clues {
    x: Int = 1
    y: Int = 2
  }"""
        )
    }
  }

  test("failures with nested clues are rendered correctly") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(nested)")
        assertInlineSnapshot(
          actual,
          """- (nested) 0ms
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(clue(List(clue(x), clue(y))) == List(x, x))

  Clues {
    x: Int = 1
    y: Int = 2
    List(clue(x), clue(y)): List[Int] = List(1, 2)
  }"""
        )
    }
  }

  test("failures with identical clue expressions are rendered correctly") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(map)")

        assertInlineSnapshot(
          actual,
          """- (map) 0ms
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(List(x, y).map(v => clue(v)) == List(x, x))

  Clues {
    v: Int = 1
    v: Int = 2
  }"""
        )
    }
  }
  test("failures in expect.all are reported with their source code") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(all)")
        assertInlineSnapshot(
          actual,
          """- (all) 0ms
 [0] assertion failed (src/main/DogFoodTests.scala:5)
 [0] 
 [0] clue(x) == clue(y)
 [0] 
 [0] Clues {
 [0]   x: Int = 1
 [0]   y: Int = 2
 [0] }

 [1] assertion failed (src/main/DogFoodTests.scala:5)
 [1] 
 [1] clue(y) == clue(z)
 [1] 
 [1] Clues {
 [1]   y: Int = 2
 [1]   z: Int = 3
 [1] }"""
        )
    }
  }

  test("values of clues are rendered with the given show") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(show)")
        assertInlineSnapshot(
          actual,
          """- (show) 0ms
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(clue(x) == clue(y))

  Clues {
    x: Int = int-1
    y: Int = int-2
  }"""
        )
    }
  }

  test("values of clues are rendered with show constructed from toString if no show is given") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(show-from-to-string)")
        assertInlineSnapshot(
          actual,
          """- (show-from-to-string) 0ms
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(clue(x) == clue(y))

  Clues {
    x: Foo = foo-1
    y: Foo = foo-2
  }"""
        )
    }
  }
  test("clue calls are replaced when using helper objects") {
    _.runSuite(Meta.Clue).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(helpers)")
        assertInlineSnapshot(
          actual,
          """- (helpers) 0ms
  assertion failed (src/main/DogFoodTests.scala:5)

  expect(CustomHelpers.clue(x) == otherclue(y) || x == clue(z))

  Clues {
    x: Int = 1
    y: Int = 2
    z: Int = 3
  }"""
        )
    }
  }

  test("expect.same source locations are rendered correctly") {
    _.runSuite(Meta.SourceLocationSuite).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(expect-same)")
        if (ScalaCompat.isScala3) {
          assertInlineSnapshot(
            actual,
            """- (expect-same) 0ms
  Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:22)

  => Diff (- obtained, + expected)
  -2
  +1

  modules/framework-cats/shared/src/test/scala/Meta.scala:22
        expect.same(x, y)
                        ^"""
          )
        } else {
          assertInlineSnapshot(
            actual,
            """- (expect-same) 0ms
  Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:22)

  => Diff (- obtained, + expected)
  -2
  +1

  modules/framework-cats/shared/src/test/scala/Meta.scala:22
        expect.same(x, y)
                   ^"""
          )
        }
    }
  }

  test("multiple expectations on the same source line are rendered correctly") {
    _.runSuite(Meta.SourceLocationSuite).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(multiple)")
        if (ScalaCompat.isScala3) {
          assertInlineSnapshot(
            actual,
            """- (multiple) 0ms
 [0] Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:29)
 [0] 
 [0] => Diff (- obtained, + expected)
 [0] -2
 [0] +1
 [0] 
 [0] modules/framework-cats/shared/src/test/scala/Meta.scala:29
 [0]       expect.same(x, y) && expect.same(y, z)
 [0]                       ^

 [1] Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:29)
 [1] 
 [1] => Diff (- obtained, + expected)
 [1] -3
 [1] +2
 [1] 
 [1] modules/framework-cats/shared/src/test/scala/Meta.scala:29
 [1]       expect.same(x, y) && expect.same(y, z)
 [1]                                            ^"""
          )
        } else {
          assertInlineSnapshot(
            actual,
            """- (multiple) 0ms
 [0] Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:29)
 [0] 
 [0] => Diff (- obtained, + expected)
 [0] -2
 [0] +1
 [0] 
 [0] modules/framework-cats/shared/src/test/scala/Meta.scala:29
 [0]       expect.same(x, y) && expect.same(y, z)
 [0]                  ^

 [1] Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:29)
 [1] 
 [1] => Diff (- obtained, + expected)
 [1] -3
 [1] +2
 [1] 
 [1] modules/framework-cats/shared/src/test/scala/Meta.scala:29
 [1]       expect.same(x, y) && expect.same(y, z)
 [1]                                       ^"""
          )
        }
    }
  }

  test("traced source locations are rendered correctly") {
    _.runSuite(Meta.SourceLocationSuite).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(traced)")
        if (ScalaCompat.isScala3) {
          assertInlineSnapshot(
            actual,
            """- (traced) 0ms
  Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:33)
 (modules/framework-cats/shared/src/test/scala/Meta.scala:40)
 (modules/framework-cats/shared/src/test/scala/Meta.scala:37)

  => Diff (- obtained, + expected)
  -2
  +1

  modules/framework-cats/shared/src/test/scala/Meta.scala:33
        helper
             ^
  modules/framework-cats/shared/src/test/scala/Meta.scala:40
        expect.same(1, 2).traced(here)
                                    ^
  modules/framework-cats/shared/src/test/scala/Meta.scala:37
        nestedHelper.traced(here)
                               ^"""
          )
        } else {
          assertInlineSnapshot(
            actual,
            """- (traced) 0ms
  Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:33)
 (modules/framework-cats/shared/src/test/scala/Meta.scala:40)
 (modules/framework-cats/shared/src/test/scala/Meta.scala:37)

  => Diff (- obtained, + expected)
  -2
  +1

  modules/framework-cats/shared/src/test/scala/Meta.scala:33
        helper
        ^
  modules/framework-cats/shared/src/test/scala/Meta.scala:40
        expect.same(1, 2).traced(here)
                                 ^
  modules/framework-cats/shared/src/test/scala/Meta.scala:37
        nestedHelper.traced(here)
                            ^"""
          )
        }
    }
  }

  test("source locations with interpolators are rendered without warnings") {
    _.runSuite(Meta.SourceLocationSuite).flatMap {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(interpolator)")
        if (ScalaCompat.isScala3) {
          assertInlineSnapshot(
            actual,
            """- (interpolator) 0ms
  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:44)

  expect(x == "2")

  Use the `clue` function to troubleshoot

  modules/framework-cats/shared/src/test/scala/Meta.scala:44
        forEach(Option(s"$x"))(x => expect(x == "2"))
                                                   ^"""
          )
        } else {
          assertInlineSnapshot(
            actual,
            """- (interpolator) 0ms
  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:44)

  expect(x == "2")

  Use the `clue` function to troubleshoot

  modules/framework-cats/shared/src/test/scala/Meta.scala:44
        forEach(Option(s"$x"))(x => expect(x == "2"))
                                          ^"""
          )
        }
    }
  }

  private def outputBeforeFailures(logs: Chain[LoggedEvent]): Chain[String] = {
    logs
      .takeWhile {
        case LoggedEvent.Info(s) if s.contains("FAILURES") => false
        case _                                             => true
      }
      .collect {
        case LoggedEvent.Info(s)  => s
        case LoggedEvent.Debug(s) => s
        case LoggedEvent.Warn(s)  => s
        case LoggedEvent.Error(s) => s
      }
      .map(Colours.removeASCIIColors)
      .map(_.trim)
  }

  private def extractLogEventBeforeFailures(logs: Chain[LoggedEvent])(
      pf: PartialFunction[LoggedEvent, String]): Option[String] = {
    logs
      .takeWhile {
        case LoggedEvent.Info(s) if s.contains("FAILURES") => false
        case _                                             => true
      }
      .collectFirst(pf)
      .map(Colours.removeASCIIColors)
      .map(_.trim)
  }

  private def extractFailureMessageForTest(
      logs: Chain[LoggedEvent],
      testName: String): String =
    extractLogEventAfterFailures(logs) {
      case LoggedEvent.Error(msg) if msg.contains(testName) =>
        msg
    }.get

  private def extractLogEventAfterFailures(logs: Chain[LoggedEvent])(
      pf: PartialFunction[LoggedEvent, String]): Option[String] = {
    logs
      .dropWhile {
        case LoggedEvent.Info(s) if s.contains("FAILURES") => false
        case _                                             => true
      }
      .collectFirst(pf)
      .map(Colours.removeASCIIColors)
      .map(_.trim)
  }
}
