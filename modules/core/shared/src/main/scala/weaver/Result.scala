package weaver

import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }

private[weaver] sealed trait Result {
  def formatted: Option[String]
}

private[weaver] object Result {
  import Formatter._

  def fromAssertion(
      sourceLocationUrl: Option[String],
      assertion: Expectations): Result =
    assertion.run match {
      case Valid(_)        => Success
      case Invalid(failed) =>
        Failures(failed.map(ex =>
          Failures.Failure(ex.message, ex, sourceLocationUrl, ex.locations)))
    }

  case object Success extends Result {
    def formatted: Option[String] = None
  }

  final case class Ignored(
      reason: String,
      sourceLocationUrl: Option[String],
      location: SourceLocation)
      extends Result {

    def formatted: Option[String] = {
      Some(formatDescription(reason,
                             sourceLocationUrl = sourceLocationUrl,
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
          failures.head.sourceLocationUrl,
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
              sourceLocationUrl,
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
        source: ExpectationFailed,
        sourceLocationUrl: Option[String],
        locations: NonEmptyList[SourceLocation])
  }

  final case class OnlyTagNotAllowedInCI(
      location: SourceLocation)
      extends Result {

    def formatted: Option[String] = {
      val formattedMessage = formatDescription(
        "'Only' tag is not allowed when `isCI=true`",
        None,
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

  def from(sourceLocationUrl: Option[String], error: Throwable): Result = {
    error match {
      case ex: IgnoredException =>
        Ignored(ex.reason, sourceLocationUrl, ex.location)
      case exs: ExpectationsFailed =>
        Failures(exs.failures.map { ex =>
          Failures.Failure(ex.message, ex, sourceLocationUrl, ex.locations)
        })
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
                          None,
                          Nil,
                          Console.RED,
                          TAB2.prefix)
      } else ""
    }

    val formattedMessage = formatDescription(
      msg,
      None,
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
      sourceLocationUrl: Option[String],
      location: List[SourceLocation],
      color: String,
      prefix: String): String = {

    val prefixIsWhitespace = prefix.trim.isEmpty
    val footer             = locationFooter(sourceLocationUrl, location)
    val lines = (message.split("\\r?\\n") ++ footer).zipWithIndex.map {
      case (line, index) =>
        val linePrefix =
          if (prefixIsWhitespace && line.trim.isEmpty) "" else prefix
        if (index == 0)
          color + linePrefix + line +
            location
              .map(l => s" (${formatLocationPath(sourceLocationUrl, l)})")
              .mkString("\n")
        else
          color + linePrefix + line
    }

    lines.mkString(EOL) + Console.RESET
  }

  private def locationFooter(
      sourceLocationUrl: Option[String],
      locations: List[SourceLocation]): List[String] = {
    val lines = locations.flatMap { l =>
      l.sourceCode.fold(List.empty[String]) { sourceCode =>
        val pointer = " " * (sourceCode.column - 1) + "^"
        List(formatLocationPath(sourceLocationUrl, l),
             sourceCode.sourceLine,
             pointer)
      }
    }
    if (lines.nonEmpty) "" :: lines else Nil
  }

  private def formatLocationPath(
      sourceLocationUrl: Option[String],
      l: SourceLocation): String =
    sourceLocationUrl match {
      case Some(url) =>
        // Display a URL to a source location on a CI host. Line numbers are typically referenced with #L anchors.
        s"${url}${l.fileRelativePath}#L${l.line}"
      case None =>
        // Display a path to a local file.
        s"${l.fileRelativePath}:${l.line}"
    }

}
