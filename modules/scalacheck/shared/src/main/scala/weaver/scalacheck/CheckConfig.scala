package weaver
package scalacheck

import org.scalacheck.rng.Seed

case class CheckConfig private (
    val minimumSuccessful: Int,
    val maximumDiscardRatio: Int,
    val maximumGeneratorSize: Int,
    val perPropertyParallelism: Int,
    val initialSeed: Option[Seed]) {
  assert(maximumDiscardRatio >= 0)
  assert(maximumDiscardRatio <= 100)
  assert(minimumSuccessful > 0)

  def maximumDiscarded = minimumSuccessful * maximumDiscardRatio / 100

  def withMinimumSuccessful(minimumSuccessful: Int) = copy(
    minimumSuccessful = minimumSuccessful
  )

  def withMaximumDiscardRatio(maximumDiscardRatio: Int) = copy(
    maximumDiscardRatio = maximumDiscardRatio
  )

  def withMaximumGeneratorSize(maximumGeneratorSize: Int) = copy(
    maximumGeneratorSize = maximumGeneratorSize
  )

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
}
