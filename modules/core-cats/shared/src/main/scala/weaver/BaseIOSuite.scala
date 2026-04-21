package weaver

import cats.effect.IO

trait BaseIOSuite extends EffectSuite[IO] with BaseCatsSuite {
  protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
}
