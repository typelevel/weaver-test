package weaver
package junit

import cats.Show
import cats.Eq
import cats.effect._

import org.junit.runner.Description
import org.junit.runner.notification.{ Failure, RunListener, RunNotifier }

object JUnitRunnerTests extends SimpleIOSuite {

  implicit val showNotifList: Show[List[Notification]] =
    list => list.map(_.toString()).mkString("\n")
  implicit val eqNotification: Eq[Notification] = Eq.fromUniversalEquals

  test("Notifications are issued correctly") {
    run(Meta.MySuite).map { notifications =>
      val (failures, filteredNotifs) = notifications.partition {
        case TestFailure(_, _) => true
        case _                 => false
      }
      val failureMessage = failures.collect {
        case TestFailure(_, message) => message
      }

      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$MySuite$"),
        TestStarted("success(weaver.junit.Meta$MySuite$)"),
        TestFinished("success(weaver.junit.Meta$MySuite$)"),
        TestStarted("failure(weaver.junit.Meta$MySuite$)"),
        TestFinished("failure(weaver.junit.Meta$MySuite$)"),
        TestIgnored("ignore(weaver.junit.Meta$MySuite$)"),
        TestSuiteFinished("weaver.junit.Meta$MySuite$")
      )
      expect.same(filteredNotifs, expected) and
        exists(failureMessage)(s =>
          expect(s.contains("oops")) && expect(
            s.contains("Meta.scala")))
    }
  }

  test("Only tests tagged with only are ran") {
    run(Meta.Only).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$Only$"),
        TestIgnored("not only(weaver.junit.Meta$Only$)"),
        TestStarted("only(weaver.junit.Meta$Only$)"),
        TestFinished("only(weaver.junit.Meta$Only$)"),
        TestSuiteFinished("weaver.junit.Meta$Only$")
      )
      expect.same(notifications, expected)
    }
  }

  test("Tests tagged with only fail when ran on CI") {
    run(Meta.OnlyFailsOnCi).map { notifications =>
      def testFailure(name: String, lineNumber: Int, sourceCode: String) = {
        val srcPath =
          "modules/framework-cats/jvm/src/test/scala/junit/Meta.scala"
        val message = s"""- $name 0ms
          |  'Only' tag is not allowed when `isCI=true` ($srcPath:$lineNumber)
          |
          |  $srcPath:$lineNumber
          |${sourceCode.trim.stripMargin}
          |
          |""".stripMargin
        TestFailure(
          name = name + "(weaver.junit.Meta$OnlyFailsOnCi$)",
          message = message
        )
      }
      val firstSourceCode = if (ScalaCompat.isScala3)
        """|      pureTest("first only test".only) {
           |                                    ^
        """
      else
        """|      pureTest("first only test".only) {
           |                                 ^
        """
      val secondSourceCode = if (ScalaCompat.isScala3)
        """|      pureTest("second only test".only) {
           |                                     ^
        """
      else
        """|      pureTest("second only test".only) {
           |                                  ^
        """
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$OnlyFailsOnCi$"),
        TestIgnored("normal test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestIgnored("not only(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestStarted("first only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        testFailure("first only test", 46, firstSourceCode),
        TestFinished("first only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestStarted("second only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        testFailure("second only test", 50, secondSourceCode),
        TestFinished("second only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestSuiteFinished("weaver.junit.Meta$OnlyFailsOnCi$")
      )
      expect.eql(expected, notifications)
    }
  }

  test("Only tests tagged with only are ran (unless also tagged ignored)") {

    run(Meta.IgnoreAndOnly).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$IgnoreAndOnly$"),
        TestIgnored("not tagged(weaver.junit.Meta$IgnoreAndOnly$)"),
        TestIgnored("is ignored(weaver.junit.Meta$IgnoreAndOnly$)"),
        TestIgnored("only and ignored(weaver.junit.Meta$IgnoreAndOnly$)"),
        TestStarted("only(weaver.junit.Meta$IgnoreAndOnly$)"),
        TestFinished("only(weaver.junit.Meta$IgnoreAndOnly$)"),
        TestSuiteFinished("weaver.junit.Meta$IgnoreAndOnly$")
      )
      expect.same(expected, notifications)
    }
  }

  test("Tests tagged with ignore are ignored") {
    run(Meta.Ignore).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$Ignore$"),
        TestIgnored("is ignored(weaver.junit.Meta$Ignore$)"),
        TestStarted("not ignored 1(weaver.junit.Meta$Ignore$)"),
        TestFinished("not ignored 1(weaver.junit.Meta$Ignore$)"),
        TestStarted("not ignored 2(weaver.junit.Meta$Ignore$)"),
        TestFinished("not ignored 2(weaver.junit.Meta$Ignore$)"),
        TestSuiteFinished("weaver.junit.Meta$Ignore$")
      )
      expect.same(notifications, expected)
    }
  }

  test("Tests tagged with ignore are ignored (FunSuite)") {
    runPure(Meta.IgnorePure).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$IgnorePure$"),
        TestIgnored("is ignored(weaver.junit.Meta$IgnorePure$)"),
        TestStarted("not ignored 1(weaver.junit.Meta$IgnorePure$)"),
        TestFinished("not ignored 1(weaver.junit.Meta$IgnorePure$)"),
        TestStarted("not ignored 2(weaver.junit.Meta$IgnorePure$)"),
        TestFinished("not ignored 2(weaver.junit.Meta$IgnorePure$)"),
        TestSuiteFinished("weaver.junit.Meta$IgnorePure$")
      )
      expect.same(notifications, expected)
    }
  }

  test(
    "Even if all tests are ignored, will fail if a test is tagged with only") {

    run(Meta.OnlyFailsOnCiEvenIfIgnored).map { notifications =>
      val testFailure = {
        val srcPath =
          "modules/framework-cats/jvm/src/test/scala/junit/Meta.scala"
        val name       = "only and ignored"
        val lineNumber = 110
        val sourceCode = if (ScalaCompat.isScala3)
          """
              |      pureTest("only and ignored".only.ignore) {
              |                                     ^
            """
        else """
                 |      pureTest("only and ignored".only.ignore) {
                 |                                  ^
               """
        val message = s"""- $name 0ms
          |  'Only' tag is not allowed when `isCI=true` ($srcPath:$lineNumber)
          |
          |  $srcPath:$lineNumber
          |${sourceCode.trim.stripMargin}
          |
          |""".stripMargin
        TestFailure(
          name = name + "(weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$)",
          message = message
        )
      }

      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$"),
        TestStarted(
          "only and ignored(weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$)"),
        testFailure,
        TestFinished(
          "only and ignored(weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$)"),
        TestSuiteFinished("weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$")
      )
      expect.eql(notifications, expected)
    }
  }

  test("Works when suite asks for global resources") {
    run(classOf[Meta.Sharing]).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$Sharing"),
        TestStarted("foo(weaver.junit.Meta$Sharing)"),
        TestFinished("foo(weaver.junit.Meta$Sharing)"),
        TestSuiteFinished("weaver.junit.Meta$Sharing")
      )
      expect.same(notifications, expected)
    }
  }

  def run(suite: Class[_]): IO[List[Notification]] = for {
    runner   <- IO(new WeaverRunner(suite))
    queue    <- IO(scala.collection.mutable.Queue.empty[Notification])
    notifier <- IO(new RunNotifier())
    _        <- IO(notifier.addListener(new NotificationListener(queue)))
    _        <- IO.blocking(runner.run(notifier))
  } yield queue.toList

  def run(
      suite: SimpleIOSuite): IO[List[Notification]] =
    run(suite.getClass())

  def runPure(
      suite: FunSuite): IO[List[Notification]] =
    run(suite.getClass())

  sealed trait Notification
  case class TestSuiteStarted(name: String)             extends Notification
  case class TestAssumptionFailure(failure: Failure)    extends Notification
  case class TestFailure(name: String, message: String) extends Notification
  case class TestFinished(name: String)                 extends Notification
  case class TestIgnored(name: String)                  extends Notification
  case class TestStarted(name: String)                  extends Notification
  case class TestSuiteFinished(name: String)            extends Notification

  class NotificationListener(
      queue: scala.collection.mutable.Queue[Notification])
      extends RunListener {
    override def testSuiteStarted(description: Description): Unit =
      queue += TestSuiteStarted(description.getDisplayName())
    override def testAssumptionFailure(failure: Failure): Unit =
      queue += TestAssumptionFailure(failure)
    override def testFailure(failure: Failure): Unit =
      queue += TestFailure(failure.getDescription.getDisplayName,
                           failure.getMessage())
    override def testFinished(description: Description): Unit =
      queue += TestFinished(description.getDisplayName())
    override def testIgnored(description: Description): Unit =
      queue += TestIgnored(description.getDisplayName())
    override def testStarted(description: Description): Unit =
      queue += TestStarted(description.getDisplayName())
    override def testSuiteFinished(description: Description): Unit =
      queue += TestSuiteFinished(description.getDisplayName())
  }

}
