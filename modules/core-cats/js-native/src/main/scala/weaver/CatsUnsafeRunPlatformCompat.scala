package weaver

import cats.effect.IO
import scala.concurrent.Future
import cats.effect.unsafe.implicits.global

private[weaver] trait CatsUnsafeRunPlatformCompat extends UnsafeRun[IO] {
  def unsafeRunToFuture(task: IO[Unit]): Future[Unit] = task.unsafeToFuture()

}
