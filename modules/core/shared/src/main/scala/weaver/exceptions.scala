package weaver

import cats.data.NonEmptyList

sealed abstract class WeaverTestException(
    message: String,
    cause: Option[Throwable]
) extends RuntimeException(message, cause.orNull)

final case class AssertionException(
    message: String,
    locations: NonEmptyList[SourceLocation])
    extends WeaverTestException(message, None)

final class IgnoredException(
    val reason: Option[String],
    val location: SourceLocation)
    extends WeaverTestException(reason.orNull, None)

final class CanceledException(
    val reason: Option[String],
    val location: SourceLocation)
    extends WeaverTestException(reason.orNull, None)
