package weaver
package framework
package test

import org.typelevel.scalaccompat.annotation._
import cats.Show

import cats.effect._
// The build tool will only detect and run top-level test suites. We can however nest objects
// that contain failing tests, to allow for testing the framework without failing the build
// because the framework will have ran the tests on its own.
object Meta {

  object SourceLocationSuite extends SimpleIOSuite {

    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun

    pureTest("(expect-same)") {
      val x = 1
      val y = 2
      expect.same(x, y)
    }

    pureTest("(multiple)") {
      val x = 1
      val y = 2
      val z = 3
      expect.same(x, y) && expect.same(y, z)
    }

    pureTest("(traced)") {
      helper
    }

    def helper(implicit loc: SourceLocation): Expectations =
      nestedHelper.traced(here)

    def nestedHelper(implicit loc: SourceLocation): Expectations =
      expect.same(1, 2).traced(here)

    pureTest("(interpolator)") {
      val x = 1
      forEach(Option(s"$x"))(x => expect(x == "2"))
    }
  }

  object MutableSuiteTest extends MutableSuiteTest

  object Boom extends Error("Boom") with scala.util.control.NoStackTrace

  @nowarn2("cat=w-flag-dead-code")
  object CrashingSuite extends SimpleIOSuite {
    throw Boom
  }

  object Rendering extends SimpleIOSuite {
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

    pureTest("lots\nof\nmultiline\n(success)") {
      expect(1 == 1)
    }

    pureTest("lots\nof\nmultiline\n(failure)") {
      val x = 1
      val y = 2
      expect(clue(x) == y)
    }

    test("lots\nof\nmultiline\n(ignored)") {
      ignore("Ignore me")
    }

    test("lots\nof\nmultiline\n(cancelled)") {
      cancel("I was cancelled :(")
    }

    import cats.Show
    case class Foo(s: String, i: Int)
    object Foo {
      val show: Show[Foo] = Show.show[Foo] {
        case Foo(s, i) =>
          s"""
          |Foo {
          |  s: ${Show[String].show(s)}
          |  i: ${Show[Int].show(i)}
          |}
          """.stripMargin.trim()
      }
      implicit val comparison: Comparison[Foo] =
        Comparison.fromEq[Foo](cats.Eq.fromUniversalEquals, show)
    }

    pureTest("(eql Comparison)") {
      expect.eql(Foo("foo", 1), Foo("foo", 2))
    }
    pureTest("(same Comparison)") {
      expect.same(Foo("foo", 1), Foo("foo", 2))
    }

    pureTest("(eql Show)") {
      implicit val showForString: Show[String] = _.toLowerCase
      expect.eql("FOO", "foo")
    }

    pureTest("(interpolator)") {
      val x = 1
      // The following code should not raise a "possible missing interpolator" warning
      expect(s"$x" == "2")
    }

  }

  object Clue extends SimpleIOSuite {
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

    pureTest("(success)") {
      val x = 1
      val y = 1
      expect(clue(x) == clue(y))
    }

    pureTest("(failure)") {
      val x = 1
      val y = 2
      expect(clue(x) == clue(y))
    }

    pureTest("(nested)") {
      val x = 1
      val y = 2
      expect(clue(List(clue(x), clue(y))) == List(x, x))
    }

    pureTest("(map)") {
      val x = 1
      val y = 2
      expect(List(x, y).map(v => clue(v)) == List(x, x))
    }

    pureTest("(all)") {
      val x = 1
      val y = 2
      val z = 3
      expect.all(x == x, clue(x) == clue(y), y == y, clue(y) == clue(z), z == z)
    }

    pureTest("(show)") {
      implicit val intShow: Show[Int] = i => s"int-$i"
      val x                           = 1
      val y                           = 2
      expect(clue(x) == clue(y))
    }

    pureTest("(show-from-to-string)") {
      class Foo(i: Int) {
        override def toString = s"foo-$i"
      }
      val x: Foo = new Foo(1)
      val y: Foo = new Foo(2)

      expect(clue(x) == clue(y))
    }

    pureTest("(helpers)") {
      val x = 1
      val y = 2
      val z = 3
      import Expectations.Helpers.{ clue => otherclue }
      object CustomHelpers extends Expectations.Helpers
      expect(CustomHelpers.clue(x) == otherclue(y) || x == clue(z))
    }

  }

  object FailingTestStatusReporting extends SimpleIOSuite {
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

    pureTest("I succeeded") {
      success
    }

    pureTest("I failed") {
      failure(":(")
    }

    pureTest("I succeeded again") {
      success
    }
  }

  object FailingSuiteWithLogs extends SimpleIOSuite {
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

    loggedTest("(failure)") { log =>
      val context = Map(
        "a"       -> "b",
        "token"   -> "<something>",
        "request" -> "true"
      )

      for {
        _ <- log.info("this test")
        _ <- log.error("has failed")
        _ <- log.debug("with context", context)
      } yield failure("expected")
    }

    loggedTest("(multiple-failures)") { log =>
      val context = Map(
        "a"       -> "b",
        "token"   -> "<something>",
        "request" -> "true"
      )

      for {
        _ <- log.info("this test")
        _ <- log.error("has failed")
        _ <- log.debug("with context", context)
      } yield failure("expected") && failure("another")
    }
  }

  object ErroringWithCauses extends SimpleIOSuite {
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun

    loggedTest("erroring with causes") { _ =>
      throw CustomException(
        "surfaced error",
        CustomException("first cause",
                        CustomException("root", withSnips = true),
                        withSnips = true))
    }
  }

  object ErroringWithLongPayload extends SimpleIOSuite {
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun

    val smiles = ":)" * 1024

    test("erroring with a long message: " + smiles) {
      IO.raiseError(
        CustomException(
          "surfaced error",
          withSnips = true
        )
      ).as(success)
    }
  }

  object SucceedsWithErrorInLogs extends SimpleIOSuite {
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

    loggedTest("(failure)") { log =>
      for {
        _ <- log.error(
          "error",
          cause = CustomException(
            "surfaced error",
            withSnips = true
          )
        )
      } yield failure("expected")
    }
  }

  case class CustomException(
      str: String,
      causedBy: Exception = null,
      withSnips: Boolean = false)
      extends Exception(str, causedBy) {

    val SnippedStackTrace = Array[StackTraceElement](
      new StackTraceElement("cats.effect.internals.IORuntime",
                            "run",
                            "IORuntime.scala",
                            5),
      new StackTraceElement("java.util.concurrent.Thread",
                            "execute",
                            "Thread.java",
                            45)
    )

    val preset = Array(
      new StackTraceElement("my.package.MyClass",
                            "MyMethod",
                            "DogFoodTests.scala",
                            15),
      new StackTraceElement("my.package.ClassOfDifferentLength",
                            "method$new$1",
                            "DogFoodTests.scala",
                            20)
    )

    override def getStackTrace: Array[StackTraceElement] =
      if (withSnips) preset ++ SnippedStackTrace else preset

  }

  object TimeCop {
    implicit val sourceLocation: SourceLocation = SourceLocation(
      "src/main/DogFoodTests.scala",
      "src/main/DogFoodTests.scala",
      5,
      None)
  }

  object SetTimeUnsafeRun extends CatsUnsafeRun {
    private val setTimestamp = weaver.internals.Timestamp.localTime(12, 54, 35)

    override def realTimeMillis: IO[Long] = IO.pure(setTimestamp)
  }

}
