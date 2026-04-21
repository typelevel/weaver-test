package weaver

import weaver.junit.WeaverRunnerPlan

private[weaver] trait SharedResourceRunnableSuite[F[_]]
    extends RunnableSuite[F] {
  self: SharedResourceSuite[F] =>

  private[weaver] final def plan: WeaverRunnerPlan =
    WeaverRunnerPlan(self.analyze(self.testSeq.toList, List.empty))
}
