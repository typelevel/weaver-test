package weaver

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.scalanative.concurrent.weaver.internals.NativeExecutionContext
import scala.scalanative.meta.LinktimeInfo

import cats.effect.IO
import cats.effect.unsafe.implicits.global

private[weaver] trait CatsUnsafeRunPlatformCompat {
  self: CatsUnsafeRun =>

  def unsafeRunSync(task: IO[Unit]): Unit = {
    if (LinktimeInfo.isMultithreadingEnabled) task.unsafeRunSync()
    else {
      val future = task.unsafeToFuture()
      NativeExecutionContext.helpComplete()
      Await.result(future, 1.minute)
    }
  }

  def background(task: IO[Unit]): CancelToken = ???

}
