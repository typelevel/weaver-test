package weaver

import cats.effect.IO

trait BaseIOSuite extends RunnableSuite[IO] with BaseCatsSuite {
  protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
}
