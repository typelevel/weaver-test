package weaver

import cats.effect.{ IO }

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] with CatsUnsafeRunPlatformCompat {

  override implicit val parallel = IO.parallelForIO
  override implicit val effect   = IO.asyncForIO
}
