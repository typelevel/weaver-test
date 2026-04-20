package weaver

import org.junit.runner.RunWith

/**
 * A suite that can be executed without a provided runtime.
 *
 * This is required for JUnit integration. JUnit is used to run suites
 * individually in IntelliJ.
 */
@RunWith(classOf[weaver.junit.WeaverRunner])
private[weaver] abstract class RunnableSuite[F[_]] extends EffectSuite[F] {

  override protected def effectCompat: UnsafeRun[EffectType]
  private[weaver] def getEffectCompat: UnsafeRun[EffectType] = effectCompat

  private[weaver] def runUnsafe(report: TestOutcome => Unit): Unit =
    effectCompat.unsafeRunSync(run(List.empty)(outcome =>
      effectCompat.effect.delay(report(outcome))))

  private[weaver] def plan: junit.WeaverRunnerPlan
}
