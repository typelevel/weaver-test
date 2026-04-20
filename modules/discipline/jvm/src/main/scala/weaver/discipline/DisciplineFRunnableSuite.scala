package weaver
package discipline

import weaver.RunnableSuite
import weaver.junit.WeaverRunnerPlan

private[discipline] trait DisciplineFRunnableSuite[F[_]]
    extends RunnableSuite[F] {
  self: DisciplineFSuite[F] =>

  private[weaver] override def plan: WeaverRunnerPlan =
    foundProps.synchronized {
      WeaverRunnerPlan(Nil, self.foundProps.toList.map(_.name))
    }
}
