package weaver

import cats.data.NonEmptyList

sealed abstract class WeaverTestException private[weaver] (
    message: String
) extends RuntimeException(message)

final class ExpectationFailed(
    private[weaver] val message: String,
    private[weaver] val locations: NonEmptyList[SourceLocation]) {
  private[weaver] def withLocation(
      location: SourceLocation): ExpectationFailed =
    new ExpectationFailed(message, locations.append(location))
}

final class IgnoredException private[weaver] (
    private[weaver] val reason: String,
    private[weaver] val location: SourceLocation)
    extends WeaverTestException(reason)

final class ExpectationsFailed(
    private[weaver] val failures: NonEmptyList[ExpectationFailed])
    extends WeaverTestException(
      s"One or more expecations failed:\n${failures.map(_.message).toList.mkString(System.lineSeparator)}")
