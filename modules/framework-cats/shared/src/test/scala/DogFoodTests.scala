package weaver
package framework
package test

import cats.data.Chain
import cats.effect.{ IO, Resource }
import cats.syntax.all._

import sbt.testing.Status.Error

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
    _.runSuite(Meta.FailingTestStatusReporting).map {
      case (logs, _) =>
        val statusReport = outputBeforeFailures(logs).mkString_("\n").trim()

        val expected = """
        |weaver.framework.test.Meta$FailingTestStatusReporting
        |+ I succeeded 0ms
        |- I failed 0ms
        |+ I succeeded again 0ms
        |
        """.stripMargin.trim

        expect.same(statusReport, expected)
    }
  }

  test("test suite outputs logs for failed tests") {
    _.runSuite(Meta.FailingSuiteWithlogs).map {
      case (logs, _) =>
        val expected =
          s"""
            |- failure 0ms
            |  expected (src/main/DogFoodTests.scala:5)
            |
            |    [INFO]  12:54:35 [DogFoodTests.scala:5] this test
            |    [ERROR] 12:54:35 [DogFoodTests.scala:5] has failed
            |    [DEBUG] 12:54:35 [DogFoodTests.scala:5] with context
            |        a       -> b
            |        token   -> <something>
            |        request -> true
            |""".stripMargin.trim

        exists(extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }) { actual =>
          expect.same(actual, expected)
        }
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
    _.runSuite(Meta.ErroringWithCauses).map {
      case (logs, _) =>
        val actual = extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

        val expected =
          """
          |- erroring with causes 0ms
          |  Meta$CustomException: surfaced error
          |
          |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
          |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
          |
          |  Caused by: weaver.framework.test.Meta$CustomException: first cause
          |
          |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
          |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
          |  <snipped>                cats.effect.internals.<...>
          |  <snipped>                java.util.concurrent.<...>
          |
          |  Caused by: weaver.framework.test.Meta$CustomException: root
          |
          |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
          |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
          |  <snipped>                cats.effect.internals.<...>
          |  <snipped>                java.util.concurrent.<...>
          |
          |""".stripMargin.trim

        expect.same(actual, expected)
    }
  }

  test("failures with exceptions in logs display them correctly") {
    _.runSuite(Meta.SucceedsWithErrorInLogs).map {
      case (logs, _) =>
        val expected =
          """
             |- failure 0ms
             |  expected (src/main/DogFoodTests.scala:5)
             |
             |    [ERROR] 12:54:35 [DogFoodTests.scala:5] error
             |    weaver.framework.test.Meta$CustomException: surfaced error
             |        DogFoodTests.scala:15    my.package.MyClass#MyMethod
             |        DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
             |        <snipped>                cats.effect.internals.<...>
             |        <snipped>                java.util.concurrent.<...>
             |""".stripMargin.trim

        exists(extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }) { actual =>
          expect.same(actual, expected)
        }
    }
  }

  test("failures with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual = extractLogEventAfterFailures(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

        val expected =
          s"""
        |- lots 0ms
        |  of
        |  multiline
        |  (failure)
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:54)
        |
        |  expect(clue(x) == y)
        |
        |  Clues {
        |    x: Int = 1
        |  }
        |
        |  54:      expect(clue(x) == y)
        """.stripMargin.trim

        expect.same(actual, expected)
    }
  }

  test("successes with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual =
          extractLogEventBeforeFailures(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(success)") => msg
          }.get

        val expected = """
        |+ lots 0ms
        |  of
        |  multiline
        |  (success)
        """.stripMargin.trim

        expect.same(actual, expected)
    }
  }

  test("ignored tests with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual =
          extractLogEventBeforeFailures(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(ignored)") => msg
          }.get

        val expected = """
        |- lots 0ms
        |  of
        |  multiline
        |  (ignored) !!! IGNORED !!!
        |  Ignore me (src/main/DogFoodTests.scala:5)
        """.stripMargin.trim

        expect.same(actual, expected)
    }
  }

  test(
    "cancelled tests with multi-line test name are rendered correctly") {
    _.runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual =
          extractLogEventBeforeFailures(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(cancelled)") => msg
          }.get

        val expected = """
        |- lots 0ms
        |  of
        |  multiline
        |  (cancelled) !!! CANCELLED !!!
        |  I was cancelled :( (src/main/DogFoodTests.scala:5)
        """.stripMargin.trim

        expect.same(actual, expected)
    }
  }

  test(
    "expect.eql delegates to Comparison show when an instance is found") {
    _.runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(eql Comparison)")

        val expected = """
        |- (eql Comparison) 0ms
        |  Values not equal: (src/main/DogFoodTests.scala:5)
        |
        |  => Diff (- obtained, + expected)
        |     s: foo
        |  -  i: 2
        |  +  i: 1
        |   }""".stripMargin.trim
        expect.same(actual, expected)
    }
  }

  test(
    "expect.same delegates to Comparison show when an instance is found") {
    _.runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(same Comparison)")

        val expected = """
        |- (same Comparison) 0ms
        |  Values not equal: (src/main/DogFoodTests.scala:5)
        |
        |  => Diff (- obtained, + expected)
        |     s: foo
        |  -  i: 2
        |  +  i: 1
        |   }""".stripMargin.trim
        expect.same(actual, expected)
    }
  }

  test("successes with clues are rendered correctly") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractLogEventBeforeFailures(logs) {
          case LoggedEvent.Info(msg) if msg.contains("(success)") =>
            msg
        }.get

        val expected = s"""
        |+ (success) 0ms
        |
        """.stripMargin.trim

        expect.same(expected, actual)
    }
  }

  test("failures with clues are rendered correctly") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(failure)")
        val expected =
          s"""
        |- (failure) 0ms
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:103)
        |
        |  expect(clue(x) == clue(y))
        |
        |  Clues {
        |    x: Int = 1
        |    y: Int = 2
        |  }
        |
        |  103:      expect(clue(x) == clue(y))
        """.stripMargin.trim

        expect.same(expected, actual)
    }
  }

  test("failures with nested clues are rendered correctly") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(nested)")
        val expected =
          s"""
        |- (nested) 0ms
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:109)
        |
        |  expect(clue(List(clue(x), clue(y))) == List(x, x))
        |
        |  Clues {
        |    x: Int = 1
        |    y: Int = 2
        |    List(clue(x), clue(y)): List[Int] = List(1, 2)
        |  }
        |
        |  109:      expect(clue(List(clue(x), clue(y))) == List(x, x))
        """.stripMargin.trim

        expect.same(expected, actual)
    }
  }

  test("failures with identical clue expressions are rendered correctly") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(map)")

        val expected =
          s"""
        |- (map) 0ms
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:115)
        |
        |  expect(List(x, y).map(v => clue(v)) == List(x, x))
        |
        |  Clues {
        |    v: Int = 1
        |    v: Int = 2
        |  }
        |
        |  115:      expect(List(x, y).map(v => clue(v)) == List(x, x))
        """.stripMargin.trim

        expect.same(expected, actual)
    }
  }
  test("failures in expect.all are reported with their source code") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(all)")
        val expected =
          s"""
        |- (all) 0ms
        | [0] assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:122)
        | [0] 
        | [0] clue(x) == clue(y)
        | [0] 
        | [0] Clues {
        | [0]   x: Int = 1
        | [0]   y: Int = 2
        | [0] }
        |
        | [1] assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:122)
        | [1] 
        | [1] clue(y) == clue(z)
        | [1] 
        | [1] Clues {
        | [1]   y: Int = 2
        | [1]   z: Int = 3
        | [1] }
        """.stripMargin.trim
        expect.same(expected, actual)
    }
  }

  test("values of clues are rendered with the given show") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(show)")

        val expected =
          s"""
        |- (show) 0ms
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:129)
        |
        |  expect(clue(x) == clue(y))
        |
        |  Clues {
        |    x: Int = int-1
        |    y: Int = int-2
        |  }
        |
        |  129:      expect(clue(x) == clue(y))
        """.stripMargin.trim

        expect.same(expected, actual)
    }
  }

  test("values of clues are rendered with show constructed from toString if no show is given") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(show-from-to-string)")

        val expected =
          s"""
        |- (show-from-to-string) 0ms
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:139)
        |
        |  expect(clue(x) == clue(y))
        |
        |  Clues {
        |    x: Foo = foo-1
        |    y: Foo = foo-2
        |  }
        |
        |  139:      expect(clue(x) == clue(y))
        |
        """.stripMargin.trim

        expect.same(expected, actual)
    }
  }
  test("clue calls are replaced when using helper objects") {
    _.runSuite(Meta.Clue).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(helpers)")

        val expected =
          s"""
        |- (helpers) 0ms
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:148)
        |
        |  expect(CustomHelpers.clue(x) == otherclue(y) || x == clue(z))
        |
        |  Clues {
        |    x: Int = 1
        |    y: Int = 2
        |    z: Int = 3
        |  }
        |
        |  148:      expect(CustomHelpers.clue(x) == otherclue(y) || x == clue(z))
        """.stripMargin.trim

        expect.same(expected, actual)
    }
  }

  test("expect.same source locations are rendered correctly") {
    _.runSuite(Meta.SourceLocationSuite).map {
      case (logs, _) =>
        val actual = extractFailureMessageForTest(logs, "(expect-same)")

        val expected =
          s"""
        |- (expect-same) 0ms
        |  Values not equal: (modules/framework-cats/shared/src/test/scala/Meta.scala:22)
        |
        |  => Diff (- obtained, + expected)
        |  -2
        |  +1
        |
        |  22:      expect.same(x, y)
        """.stripMargin.trim

        expect.same(expected, actual)
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
