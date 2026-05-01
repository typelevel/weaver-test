package weaver

import cats.effect.{ IO, Resource }

trait BaseCatsSuite extends EffectSuite[IO]

/**
 * Extend this when each test in the suite returns an `Resource[IO, Res]` for
 * some shared resource `Res`
 */
abstract class IOSuite
    extends MutableFSuite[IO]
    with BaseIOSuite
    with Expectations.Helpers

/**
 * Extend this when each test in the suite returns an `IO[_]`
 */
abstract class SimpleIOSuite extends IOSuite {
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure[IO, Unit](())
}

/**
 * Extend this when each test in the suite is pure and does not return `IO[_]`
 */
trait FunSuite extends FunSuiteF[IO] with BaseIOSuite
    with Expectations.Helpers
