package weaver
package framework

import cats.data.Chain
import cats.syntax.all._
import org.typelevel.scalaccompat.annotation.unused

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(
      runner: WeaverRunner[F],
      eventHandler: sbt.testing.EventHandler,
      logger: sbt.testing.Logger,
      @unused maxParallelism: Int)(tasks: List[sbt.testing.Task]): F[Unit] = {
    tasks.traverse { task =>
      self.framework.unsafeRun.fromFuture {
        task.asInstanceOf[AsyncTask].executeFuture(eventHandler, Array(logger))
      }
    }.map { _ =>
      Reporter.logRunFinished(Array(logger))(
        Chain(runner.failedTests.toSeq: _*))
    }
  }

  def done(runner: sbt.testing.Runner): F[String] = effect.delay(runner.done())

}
