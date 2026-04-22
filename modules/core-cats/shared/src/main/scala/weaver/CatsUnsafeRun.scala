package weaver

import scala.concurrent.Future

import cats.effect.unsafe.implicits.global
import cats.effect.{ FiberIO, IO }
import cats.effect.std.Env

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] with CatsUnsafeRunPlatformCompat {

  type CancelToken = FiberIO[Unit]

  override implicit val parallel = IO.parallelForIO
  override implicit val effect   = IO.asyncForIO
  override def env: Env[IO]      = IO.envForIO

  def cancel(token: CancelToken): Unit = unsafeRunSync(token.cancel)

  def unsafeRunToFuture(task: IO[Unit]): Future[Unit] = task.unsafeToFuture()

}
