package weaver

private[weaver] trait UnsafeRunPlatform[F[_]] extends EffectCompat[F] {

  type CancelToken

  def background(task: F[Unit]): CancelToken
  def cancel(token: CancelToken): Unit
  def unsafeRunSync(task: F[Unit]): Unit
}
