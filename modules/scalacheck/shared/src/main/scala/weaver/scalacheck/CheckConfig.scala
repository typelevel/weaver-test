package weaver
package scalacheck

import org.scalacheck.rng.Seed
import org.typelevel.scalaccompat.annotation.unused

case class CheckConfig private (
    val minimumSuccessful: Int,
    val maximumDiscardRatio: Int,
    val maximumGeneratorSize: Int,
    val perPropertyParallelism: Int,
    val initialSeed: Option[Seed]) {
  assert(maximumDiscardRatio >= 0)
  assert(maximumDiscardRatio <= 100)
  assert(minimumSuccessful > 0)

  /**
   * The maximum number of values that can be discarded by the generator. The
   * test will fail if the generator discards more values.
   */
  def maximumDiscarded = minimumSuccessful * maximumDiscardRatio / 100

  /** The number of successful runs required for a test to succeed */
  def withMinimumSuccessful(minimumSuccessful: Int) = copy(
    minimumSuccessful = minimumSuccessful
  )

  /**
   * The proportion of values discarded by the generator allowed before the test
   * is considered failed.
   *
   * A value is considered discarded if the generator outputs `None`. The
   * generator must not discard more values than the number of successful runs,
   * so this ratio is a proportion of [[minimumSuccessful]].
   */
  def withMaximumDiscardRatio(maximumDiscardRatio: Int) = copy(
    maximumDiscardRatio = maximumDiscardRatio
  )

  /** The [[org.scalacheck.Gen.Parameters.size]] of the generator. */
  def withMaximumGeneratorSize(maximumGeneratorSize: Int) = copy(
    maximumGeneratorSize = maximumGeneratorSize
  )

  /** The number of concurrent runs. */
  def withPerPropertyParallelism(perPropertyParallelism: Int) = copy(
    perPropertyParallelism = perPropertyParallelism
  )

  def withInitialSeed(initialSeed: Option[Seed]) = copy(
    initialSeed = initialSeed
  )
}

object CheckConfig {
  def default: CheckConfig = CheckConfig(
    minimumSuccessful = 80,
    maximumDiscardRatio = 5,
    maximumGeneratorSize = 100,
    perPropertyParallelism = 10,
    initialSeed = None
  )

  def apply(
      minimumSuccessful: Int,
      maximumDiscardRatio: Int,
      maximumGeneratorSize: Int,
      perPropertyParallelism: Int,
      initialSeed: Option[Seed]): CheckConfig =
    new CheckConfig(minimumSuccessful,
                    maximumDiscardRatio,
                    maximumGeneratorSize,
                    perPropertyParallelism,
                    initialSeed)

  @unused
  private def unapply(c: CheckConfig): Some[CheckConfig] = Some(c)
}
