package weaver

import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }

private[weaver] sealed trait Result {
  def formatted: Option[String]
}

private[weaver] object Result {
  import Formatter._

  def fromAssertion(assertion: Expectations): Result = assertion.run match {
    case Valid(_)        => Success
    case Invalid(failed) =>
      Failures(failed.map(ex =>
        Failures.Failure(ex.message, ex, ex.locations)))
  }

  case object Success extends Result {
    def formatted: Option[String] = None
  }

  final case class Ignored(reason: String, location: SourceLocation)
      extends Result {

    def formatted: Option[String] = {
      Some(formatDescription(reason,
                             List(location),
                             Console.YELLOW,
                             TAB2.prefix))
    }
  }

  final case class Failures(failures: NonEmptyList[Failures.Failure])
      extends Result {

    def formatted: Option[String] =
      if (failures.size == 1) {
        val failure          = failures.head
        val formattedMessage = formatDescription(
          failure.msg,
          failure.locations.toList,
          Console.RED,
          TAB2.prefix
        ) + DOUBLE_EOL
        Some(formattedMessage)
      } else {

        val descriptions = failures.zipWithIndex.map {
          case (failure, idx) =>
            import failure._

            formatDescription(
              msg,
              locations.toList,
              Console.RED,
              s" [$idx] "
            )
        }

        Some(descriptions.toList.mkString("", DOUBLE_EOL, DOUBLE_EOL))
      }
  }

  object Failures {
    final case class Failure(
        msg: String,
        source: Throwable,
        locations: NonEmptyList[SourceLocation])
  }

  final case class OnlyTagNotAllowedInCI(
      location: SourceLocation)
      extends Result {

    def formatted: Option[String] = {
      val formattedMessage = formatDescription(
        "'Only' tag is not allowed when `isCI=true`",
        List(location),
        Console.RED,
        TAB2.prefix
      ) + DOUBLE_EOL
      Some(formattedMessage)
    }
  }

  final case class Exception(source: Throwable) extends Result {

    def formatted: Option[String] = {
      val description = {
        val name      = source.getClass.getName
        val className = name.substring(name.lastIndexOf(".") + 1)
        Option(source.getMessage)
          .filterNot(_.isEmpty)
          .fold(className)(m => s"$className: $m")
      }

      Some(formatError(description, source))
    }
  }

  val success: Result = Success

  def from(error: Throwable): Result = {
    error match {
      case ex: ExpectationFailed =>
        Failures(NonEmptyList.of(Failures.Failure(
          ex.message,
          ex,
          ex.locations)))
      case ex: IgnoredException =>
        Ignored(ex.reason, ex.location)
      case other =>
        Exception(other)
    }
  }

  private def formatError(msg: String, source: Throwable): String = {

    val stackTrace = {
      val stackTraceLines = TestErrorFormatter.formatStackTrace(source, None)

      def traverseCauses(ex: Throwable): Vector[Throwable] = {
        Option(ex.getCause) match {
          case None        => Vector()
          case Some(cause) => Vector(cause) ++ traverseCauses(cause)
        }
      }

      val causes               = traverseCauses(source)
      val causeStackTraceLines = causes.flatMap { cause =>
        Vector(EOL + "Caused by: " + cause.toString + EOL) ++
          TestErrorFormatter.formatStackTrace(cause, None)
      }

      val errorOutputLines = stackTraceLines ++ causeStackTraceLines

      if (errorOutputLines.nonEmpty) {
        formatDescription(errorOutputLines.mkString(EOL),
                          Nil,
                          Console.RED,
                          TAB2.prefix)
      } else ""
    }

    val formattedMessage = formatDescription(
      msg,
      Nil,
      Console.RED,
      TAB2.prefix
    )

    var res = formattedMessage + DOUBLE_EOL
    if (stackTrace.nonEmpty) {
      res += stackTrace + DOUBLE_EOL
    }
    res
  }

  private def formatDescription(
      message: String,
      location: List[SourceLocation],
      color: String,
      prefix: String): String = {

    val prefixIsWhitespace = prefix.trim.isEmpty
    val footer             = locationFooter(location)
    val lines = (message.split("\\r?\\n") ++ footer).zipWithIndex.map {
      case (line, index) =>
        val linePrefix =
          if (prefixIsWhitespace && line.trim.isEmpty) "" else prefix
        if (index == 0)
          color + linePrefix + line +
            location
              .map(l => s" (${l.fileRelativePath}:${l.line})")
              .mkString("\n")
        else
          color + linePrefix + line
    }

    lines.mkString(EOL) + Console.RESET
  }

  private def locationFooter(locations: List[SourceLocation]): List[String] = {
    val lines = locations.flatMap { l =>
      val prefix = s"${l.fileRelativePath}:${l.line}"
      l.sourceCode.fold(List.empty[String]) { sourceCode =>
        val pointer = " " * (sourceCode.column - 1) + "^"
        List(prefix, sourceCode.sourceLine, pointer)
      }
    }
    if (lines.nonEmpty) "" :: lines else Nil
  }
}
