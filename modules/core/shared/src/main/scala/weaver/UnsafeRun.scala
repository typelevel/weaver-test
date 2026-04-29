package weaver

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import cats.Parallel
import cats.effect.Async

trait EffectCompat[F[_]] {
  implicit def parallel: Parallel[F]
  implicit def effect: Async[F]

  private[weaver] final def sleep(duration: FiniteDuration): F[Unit] =
    effect.sleep(duration)
  private[weaver] final def fromFuture[A](thunk: => Future[A]): F[A] =
    effect.fromFuture(effect.delay(thunk))
  private[weaver] final def async[A](cb: (Either[Throwable,
                                                 A] => Unit) => Unit): F[A] =
    effect.async_(cb)
}

/**
 * Abstraction allowing for running IO constructs unsafely.
 *
 * This is meant to delegate to library-specific constructs for running effect
 * types.
 */
trait UnsafeRun[F[_]] extends UnsafeRunPlatform[F]
