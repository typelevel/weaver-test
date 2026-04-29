package weaver

import cats.effect.unsafe.implicits.global
import cats.effect.{ FiberIO, IO }

private[weaver] trait CatsUnsafeRunPlatformCompat extends UnsafeRun[IO] {

  type CancelToken = FiberIO[Unit]

  def unsafeRunSync(task: IO[Unit]): Unit = task.unsafeRunSync()
  def cancel(token: CancelToken): Unit    = token.cancel.unsafeRunSync()

  def background(task: IO[Unit]): CancelToken =
    task.start.unsafeRunSync()

}
