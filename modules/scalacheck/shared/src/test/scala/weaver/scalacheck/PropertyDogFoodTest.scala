package weaver
package scalacheck

import scala.concurrent.duration._

import cats.effect.{ IO, Resource }

import weaver.framework._

import org.scalacheck.Gen
import org.scalacheck.rng.Seed

object PropertyDogFoodTest extends IOSuite {

  type Res = DogFood[IO]
  def sharedResource: Resource[IO, DogFood[IO]] =
    DogFood.make(new CatsEffect)

  test("Failed property tests get reported properly") { dogfood =>
    for {
      results <- dogfood.runSuite(Meta.FailedChecks)
      logs = results._1
    } yield {
      val errorLogs = logs.collect {
        case LoggedEvent.Error(msg) => msg
      }
      exists(errorLogs) { log =>
        val seed = Meta.FailedChecks.initialSeed.toBase64
        // Go into software engineering they say
        // Learn how to make amazing algorithms
        // Build robust and deterministic software
        val (attempt, value) =
          if (ScalaCompat.isScala3) {
            ("4", "-2147483648")
          } else {
            ("2", "0")
          }

        val actualLines = log.split(System.lineSeparator()).toList
        val expectedLines = s"""foobar
          |Property test failed on try $attempt with seed Seed.fromBase64("$seed") and input $value.
          |You can reproduce this by adding the following override to your suite:
          |
          |override def checkConfig = super.checkConfig.withInitialSeed(Seed.fromBase64("$seed").toOption)"""
          .stripMargin
          .linesIterator.toList

        forEach(actualLines.zip(expectedLines))({ case (actual, expected) =>
          expect(actual.contains(expected))
        })
      }
    }
  }

  // 100 checks sleeping 1 second each should not take 100 seconds
  test("Checks are parallelised") { dogfood =>
    for {
      events <- dogfood.runSuite(Meta.ParallelChecks).map(_._2)
      _      <- expect(events.size == 1).failFast
    } yield {
      expect(events.headOption.get.duration() < 100000)
    }
  }

  test("Config can be overridden") { dogfood =>
    val maximumDiscarded =
      Meta.ConfigOverrideChecks.configOverride.maximumDiscarded
    expectErrorMessage(
      s"Discarded more inputs (${maximumDiscarded + 1}) than allowed (${maximumDiscarded})",
      dogfood.runSuite(Meta.ConfigOverrideChecks))
  }

  test("Discarded counts should be accurate") { dogfood =>
    val maximumDiscarded = Meta.DiscardedChecks.checkConfig.maximumDiscarded
    expectErrorMessage(
      s"Discarded more inputs (${maximumDiscarded + 1}) than allowed (${maximumDiscarded})",
      dogfood.runSuite(Meta.DiscardedChecks))
  }
  test("Discard ratio of zero should still run tests") {
    dogfood =>
      expectNoErrorMessage(dogfood.runSuite(Meta.NoDiscardsChecks))
  }

  def expectErrorMessage(
      msg: String,
      state: IO[DogFood.State]): IO[Expectations] =
    state.map { case (logs, _) =>
      val errorLogs = logs.collect {
        case LoggedEvent.Error(msg) => msg
      }
      exists(errorLogs) { log =>
        expect(log.contains(msg))
      }
    }

  def expectNoErrorMessage(state: IO[DogFood.State]): IO[Expectations] =
    state.map { case (logs, _) =>
      val errorLogs = logs.collect {
        case LoggedEvent.Error(msg) => msg
      }
      expect(errorLogs.size == 0)
    }
}

object Meta {

  trait MetaSuite extends SimpleIOSuite with Checkers {
    def partiallyAppliedForall: PartiallyAppliedForall
  }

  trait ParallelChecks extends MetaSuite {

    test("sleeping forall") {
      partiallyAppliedForall { (x: Int, y: Int) =>
        IO.sleep(1.second) *> IO(expect(x + y == y + x))
      }
    }
  }

  object ParallelChecks extends ParallelChecks {

    override def partiallyAppliedForall: PartiallyAppliedForall = forall

    override def checkConfig: CheckConfig =
      super.checkConfig
        .withPerPropertyParallelism(100)
        .withMinimumSuccessful(100)
  }

  object FailedChecks extends SimpleIOSuite with Checkers {

    val initialSeed = Seed(5L)
    override def checkConfig: CheckConfig =
      super.checkConfig
        .withPerPropertyParallelism(1)
        .withInitialSeed(Some(initialSeed))

    test("foobar") {
      forall { (x: Int) =>
        expect(x > 0)
      }
    }
  }

  trait SucceededChecks extends MetaSuite {

    test("foobar") {
      partiallyAppliedForall { (_: Int) =>
        IO(expect(true))
      }
    }
  }

  trait DiscardedChecks extends MetaSuite {

    test("Discards all the time") {
      partiallyAppliedForall(Gen.posNum[Int].suchThat(_ < 0))(succeed)
    }
  }

  object ConfigOverrideChecks extends DiscardedChecks {

    val configOverride =
      super.checkConfig
        .withMinimumSuccessful(200)
        .withPerPropertyParallelism(1)

    override def partiallyAppliedForall: PartiallyAppliedForall =
      forall.withConfig(configOverride)
  }

  object DiscardedChecks extends DiscardedChecks {

    override def partiallyAppliedForall: PartiallyAppliedForall = forall

    override def checkConfig =
      super.checkConfig
        .withMinimumSuccessful(100)
        .withPerPropertyParallelism(
          1
        ) // to avoid overcounting of discarded checks
  }

  object NoDiscardsChecks extends SucceededChecks {

    override def partiallyAppliedForall: PartiallyAppliedForall = forall

    override def checkConfig =
      super.checkConfig
        .withMinimumSuccessful(5)
        // Set the discard ratio to 0. No discarded tests are permitted.
        .withMaximumDiscardRatio(0)
        .withPerPropertyParallelism(
          1
        ) // to avoid overcounting of discarded checks
  }

}
