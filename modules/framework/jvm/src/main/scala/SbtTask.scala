package weaver
package framework

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }

import cats.data.Chain

import sbt.testing.{ Event, EventHandler, Logger, Task, TaskDef }
import java.util.concurrent.TimeUnit

private[framework] class SbtTask(
    val taskDef: TaskDef,
    isDone: AtomicBoolean,
    stillRunning: AtomicInteger,
    waitForResourcesShutdown: java.util.concurrent.Semaphore,
    start: scala.concurrent.Promise[Unit],
    queue: java.util.concurrent.LinkedBlockingQueue[SuiteEvent],
    loggerPermit: java.util.concurrent.Semaphore,
    readFailed: () => Chain[(SuiteName, TestOutcome)]
) extends Task {

  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[Task] = {
    val log = Reporter.log(loggers)(_)

    start.trySuccess(())

    var finished: Boolean = false

    loggerPermit.acquire()
    try {
      while (!finished && !isDone.get()) {
        val nextEvent = Option(queue.poll(1, TimeUnit.SECONDS))

        nextEvent.foreach {
          case s @ SuiteStarted(_) => log(s)
          case SuiteFinished(_) =>
            finished = true
            if (stillRunning.decrementAndGet == 0) {
              waitForResourcesShutdown.acquire()
              log(RunFinished(readFailed()))
            }
          case t @ TestFinished(outcome) =>
            eventHandler.handle(sbtEvent(outcome))
            log(t)
        }
      }
    } finally {
      loggerPermit.release()
    }

    Array()
  }

  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger],
      continuation: Array[Task] => Unit): Unit = {
    continuation(execute(eventHandler, loggers))
  }

  def tags(): Array[String] = Array()

  private def sbtEvent(outcome: TestOutcome): Event = SbtEvent(taskDef, outcome)
}
