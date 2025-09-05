package weaver

import scala.concurrent.duration.FiniteDuration

import cats.data.Chain

import TestOutcome.Mode

trait TestOutcome {
  def name: String
  def duration: FiniteDuration
  def status: TestStatus
  def log: Chain[Log.Entry]
  def formatted(mode: Mode): String
  def cause: Option[Throwable]
}

object TestOutcome {

  sealed trait Mode
  case object Summary extends Mode
  case object Verbose extends Mode

  def apply(
      name: String,
      duration: FiniteDuration,
      result: Result,
      log: Chain[Log.Entry]): TestOutcome = Default(name, duration, result, log)

  case class Default(
      name: String,
      duration: FiniteDuration,
      result: Result,
      log: Chain[Log.Entry])
      extends TestOutcome {

    def status: TestStatus = result match {
      case Result.Success       => TestStatus.Success
      case Result.Ignored(_, _) => TestStatus.Ignored
      case Result.OnlyTagNotAllowedInCI(_) | Result.Failures(_) =>
        TestStatus.Failure
      case Result.Exception(_) => TestStatus.Exception
    }

    def cause: Option[Throwable] = result match {
      case Result.Exception(cause)   => Some(cause)
      case Result.Failures(failures) => Some(failures.head.source)
      case Result.OnlyTagNotAllowedInCI(_) | Result.Ignored(
            _,
            _) | Result.Success => None
    }

    def formatted(mode: Mode): String =
      Formatter.outcomeWithResult(this, result, mode)
  }
}
