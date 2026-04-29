package weaver

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import cats.data.Chain
import cats.effect.Ref
import cats.effect.Async
import cats.syntax.all._

object Test {

  def apply[F[_]](name: String, f: Log[F] => F[Expectations])(
      implicit F: Async[F]): F[TestOutcome] = {
    for {
      ref   <- Ref[F].of(Chain.empty[Log.Entry])
      start <- F.realTime
      res   <-
        F.defer(f(Log.collected[F, Chain](ref, F.realTime.map(_.toMillis))))
          .map(Result.fromAssertion)
          .handleError(ex => Result.from(ex))
      end  <- F.realTime
      logs <- ref.get
    } yield TestOutcome(name, end - start, res, logs)
  }

  def pure(name: String)(ex: () => Expectations): TestOutcome = {
    val start               = System.currentTimeMillis()
    val (attempt, duration) = Try(ex()) -> (System.currentTimeMillis() - start)

    val res = attempt match {
      case Success(assertions) => Result.fromAssertion(assertions)
      case Failure(e)          => Result.from(e)
    }

    TestOutcome(name, duration.millis, res, Chain.empty)
  }

  def apply[F[_]](name: String, f: F[Expectations])(
      implicit F: Async[F]
  ): F[TestOutcome] = apply[F](name, (_: Log[F]) => f)

}
