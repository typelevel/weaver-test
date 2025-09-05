package weaver
package framework

import cats.effect.implicits._

import sbt.testing._
import org.typelevel.scalaccompat.annotation.unused

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(
      @unused runner: WeaverRunner[F],
      eventHandler: EventHandler,
      logger: Logger,
      maxParallelism: Int)(tasks: List[sbt.testing.Task]): F[Unit] = {
    effect.void {
      val r = tasks.toVector.parTraverseN[F, Unit](maxParallelism) { task =>
        effect.blocking(discard[Array[Task]](task.execute(eventHandler,
                                                          Array(logger))))
      }
      r
    }
  }

  def done(runner: Runner): F[String] =
    effect.blocking(runner.done())
}
