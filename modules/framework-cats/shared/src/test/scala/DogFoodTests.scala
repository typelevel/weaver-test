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
            |- erroring with a long message: ${Meta.ErroringWithLongPayload
              .smiles} 0ms
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

        // HONESTLY.
        val (location, capturedExpression) =
          if (Platform.isScala3) (31, "1 == 2") else (32, "expect(1 == 2)")

        val expected = s"""
        |- lots 0ms
        |  of
        |  multiline
        |  (failure)
        |  assertion failed (modules/framework-cats/shared/src/test/scala/Meta.scala:$location)
        |
        |  $capturedExpression
        |
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
        val actual =
          extractLogEventAfterFailures(logs) {
            case LoggedEvent.Error(msg) if msg.contains("(eql Comparison)") =>
              msg
          }.get

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
        val actual =
          extractLogEventAfterFailures(logs) {
            case LoggedEvent.Error(msg) if msg.contains("(same Comparison)") =>
              msg
          }.get

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
