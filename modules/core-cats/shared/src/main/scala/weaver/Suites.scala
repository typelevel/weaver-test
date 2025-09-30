package weaver

import cats.effect.{ IO, Resource }

trait BaseCatsSuite extends EffectSuite.Provider[IO]

abstract class MutableIOSuite
    extends MutableFSuite[IO]
    with BaseIOSuite
    with Expectations.Helpers

abstract class SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure[IO, Unit](())
}

trait FunSuiteIO extends BaseFunIOSuite with Expectations.Helpers
