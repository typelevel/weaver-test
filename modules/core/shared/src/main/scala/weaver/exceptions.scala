package weaver

import cats.data.NonEmptyList

private[weaver] sealed abstract class WeaverTestException(
    message: String,
    cause: Option[Throwable]
) extends RuntimeException(message, cause.orNull)

private[weaver] final class AssertionException(
    private[weaver] val message: String,
    private[weaver] val locations: NonEmptyList[SourceLocation])
    extends WeaverTestException(message, None) {
  private[weaver] def withLocation(
      location: SourceLocation): AssertionException =
    new AssertionException(message, locations.append(location))
}

private[weaver] final class IgnoredException(
    private[weaver] val reason: Option[String],
    private[weaver] val location: SourceLocation)
    extends WeaverTestException(reason.orNull, None)

private[weaver] final class CanceledException(
    private[weaver] val reason: Option[String],
    private[weaver] val location: SourceLocation)
    extends WeaverTestException(reason.orNull, None)
