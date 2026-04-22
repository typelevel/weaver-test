package weaver

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import cats.Defer
import cats.data.Chain
import cats.effect.Ref
import cats.effect.Clock
import cats.effect.Concurrent
import cats.effect.std.Env
import cats.syntax.all._
import weaver.internals.SourceLocationUrl
object Test {

  def apply[F[_]](name: String, f: Log[F] => F[Expectations])(
      implicit F: Defer[F],
      G: Concurrent[F],
      C: Clock[F],
      E: Env[F]
  ): F[TestOutcome] = {
    for {
      ref       <- Ref[F].of(Chain.empty[Log.Entry])
      start     <- C.realTime
      sourceUrl <- SourceLocationUrl[F]
      res       <- Defer[F]
        .defer(f(Log.collected[F, Chain](ref, C.realTime.map(_.toMillis))))
        .map(Result.fromAssertion(sourceUrl, _))
        .handleError(ex => Result.from(sourceUrl, ex))
      end  <- C.realTime
      logs <- ref.get
    } yield TestOutcome(name, end - start, res, logs)
  }

  def pure(name: String)(ex: () => Expectations): TestOutcome = {
    val start               = System.currentTimeMillis()
    val (attempt, duration) = Try(ex()) -> (System.currentTimeMillis() - start)

    val res = attempt match {
      case Success(assertions) => Result.fromAssertion(None, assertions)
      case Failure(e)          => Result.from(None, e)
    }

    TestOutcome(name, duration.millis, res, Chain.empty)
  }

  def apply[F[_]](name: String, f: F[Expectations])(
      implicit F: Defer[F],
      G: Concurrent[F],
      C: Clock[F],
      E: Env[F]
  ): F[TestOutcome] = apply[F](name, (_: Log[F]) => f)
}
