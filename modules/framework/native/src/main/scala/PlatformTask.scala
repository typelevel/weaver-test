package weaver

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.scalanative.concurrent.weaver.internals.NativeExecutionContext
import scala.scalanative.meta.LinktimeInfo

import sbt.testing.{ EventHandler, Logger, Task }

private[weaver] trait PlatformTask extends AsyncTask {

  override def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[Task] = {
    val future = executeFuture(eventHandler, loggers)
    if (!LinktimeInfo.isMultithreadingEnabled) {
      NativeExecutionContext.helpComplete()
    }
    Await.result(future, 5.minutes)
    Array.empty[Task]
  }
}
