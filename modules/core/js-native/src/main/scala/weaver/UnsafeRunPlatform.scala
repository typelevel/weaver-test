package weaver

import scala.concurrent.Future

private[weaver] trait UnsafeRunPlatform[F[_]] extends EffectCompat[F] {
  def unsafeRunToFuture(task: F[Unit]): Future[Unit]
}
