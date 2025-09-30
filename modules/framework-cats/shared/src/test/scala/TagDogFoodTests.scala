package weaver
package framework
package test

import cats.data.Chain
import cats.effect.{ IO, Resource }

import snapshot4s.generated.snapshotConfig
import SnapshotExpectations.*

object TagDogFoodTests extends IOSuite {

  type Res = DogFood[IO]
  def sharedResource: Resource[IO, DogFood[IO]] =
    DogFood.make(new CatsEffect)

  test("tests tagged with 'only' should fail in CI") {
    _.runSuite(Meta.OnlyInCI).flatMap {
      case (logs, _) =>
        val failureMessages = logs
          .collect {
            case LoggedEvent.Error(msg) => msg
          }
          .map(Colours.removeASCIIColors)
          .map(_.trim)
          .toList
        assertInlineSnapshot(
          failureMessages,
          List(
            """- (should-fail) 0ms
  'Only' tag is not allowed when `isCI=true` (src/main/MaoTests.scala:1)""",
            """- (should-also-fail) 0ms
  'Only' tag is not allowed when `isCI=true` (src/main/MaoTests.scala:1)"""
          )
        )
    }
  }

  test("tests tagged with 'ignore' should not be run ") {
    _.runSuite(Meta.Ignored).map {
      case (_, events) => expect(events.isEmpty)
    }
  }

  test("only tests tagged with 'only' should be run") {
    _.runSuite(Meta.Only).flatMap {
      case (logs, _) =>
        assertInlineSnapshot(
          infoMessages(logs),
          List("weaver.framework.test.TagDogFoodTests$Meta$Only",
               "+ (should-run) 0ms")
        )
    }
  }

  test("test runner -o argument should be respected if no tests are tagged with 'only'") {
    _.runSuite(Meta.TestRunnerArgs,
               Array("-o", "*matches-args*")).flatMap {
      case (logs, _) =>
        assertInlineSnapshot(
          infoMessages(logs),
          List("weaver.framework.test.TagDogFoodTests$Meta$TestRunnerArgs",
               "+ (matches-args) 0ms")
        )
    }
  }

  test("test runner --only argument should be respected if no tests are tagged with 'only'") {
    _.runSuite(Meta.TestRunnerArgs,
               Array("--only", "*matches-args*")).flatMap {
      case (logs, _) =>
        assertInlineSnapshot(
          infoMessages(logs),
          List("weaver.framework.test.TagDogFoodTests$Meta$TestRunnerArgs",
               "+ (matches-args) 0ms")
        )
    }
  }

  test(
    "test runner arguments should be discarded if tests are tagged with 'only'") {
    _.runSuite(Meta.TestRunnerArgsWithOnly,
               Array("-o", "*matches-args*")).flatMap {
      case (logs, _) =>
        assertInlineSnapshot(
          infoMessages(logs),
          List(
            "weaver.framework.test.TagDogFoodTests$Meta$TestRunnerArgsWithOnly",
            "+ (does-not-match) 0ms")
        )
    }
  }
  private def infoMessages(logs: Chain[LoggedEvent]): List[String] = logs
    .collect {
      case LoggedEvent.Info(msg) => msg
    }
    .map(Colours.removeASCIIColors)
    .map(_.trim)
    .toList

  object Meta {
    implicit val sourceLocation: SourceLocation = SourceLocation(
      "src/main/MaoTests.scala",
      "src/main/MaoTests.scala",
      1,
      None)

    object OnlyInCI extends FunSuite {

      override def isCI = true
      test("(should-fail)".only) {
        expect.same(1, 1)
      }
      test("(should-also-fail)".only.ignore) {
        expect.same(1, 1)
      }
    }

    object Ignored extends FunSuite {

      test("(should-not-run)".ignore) {
        expect.same(1, 2)
      }
    }

    object Only extends SimpleIOSuite {
      override implicit protected def effectCompat: UnsafeRun[IO] =
        weaver.framework.test.Meta.SetTimeUnsafeRun

      override def isCI = false
      pureTest("(should-run)".only) {
        success
      }
      pureTest("(should-not-run)") {
        success
      }
      pureTest("(should-also-not-run)") {
        success
      }
    }

    object TestRunnerArgs extends SimpleIOSuite {
      override implicit protected def effectCompat: UnsafeRun[IO] =
        weaver.framework.test.Meta.SetTimeUnsafeRun

      pureTest("(matches-args)") {
        success
      }

      pureTest("(does-not-match)") {
        success
      }
    }

    object TestRunnerArgsWithOnly extends SimpleIOSuite {
      override implicit protected def effectCompat: UnsafeRun[IO] =
        weaver.framework.test.Meta.SetTimeUnsafeRun

      override def isCI = false

      pureTest("(matches-args)") {
        success
      }

      pureTest("(does-not-match)".only) {
        success
      }
    }
  }
}
