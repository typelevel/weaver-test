package weaver

import cats.data.NonEmptyList

private[weaver] sealed abstract class WeaverTestException(
    message: String
) extends RuntimeException(message)

private[weaver] final class AssertionException(
    private[weaver] val message: String,
    private[weaver] val locations: NonEmptyList[SourceLocation])
    extends WeaverTestException(message) {
  private[weaver] def withLocation(
      location: SourceLocation): AssertionException =
    new AssertionException(message, locations.append(location))
}

private[weaver] final class IgnoredException(
    private[weaver] val reason: String,
    private[weaver] val location: SourceLocation)
    extends WeaverTestException(reason)

private[weaver] final class CanceledException(
    private[weaver] val reason: String,
    private[weaver] val location: SourceLocation)
    extends WeaverTestException(reason)
