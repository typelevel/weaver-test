import sbt._
import sbt.internal.ProjectMatrix
import sbtprojectmatrix.ProjectMatrixPlugin.autoImport._

/** Use on ProjectMatrix rows to tag an affinity to a custom scalaVersion */
case class TargetAxis(scalaVersion: String) extends VirtualAxis.WeakAxis {

  private val scalaBinaryVersion = CrossVersion.binaryScalaVersion(scalaVersion)

  override val idSuffix = s"Target${scalaBinaryVersion.replace('.', '_')}"
  override val directorySuffix = s"target$scalaBinaryVersion"
}

object TargetAxis {

  private def targetScalaVersion(virtualAxes: Seq[VirtualAxis]): String =
    virtualAxes.collectFirst { case a: TargetAxis => a.scalaVersion }.get

  /**
   * When invoked on a ProjectMatrix with a TargetAxis, lookup the project
   * generated by `matrix` with a scalaVersion matching the one declared in that
   * TargetAxis, and resolve `key`.
   */
  def resolve[T](
      matrix: ProjectMatrix,
      key: TaskKey[T]
  ): Def.Initialize[Task[T]] =
    Def.taskDyn {
      val sv      = targetScalaVersion(virtualAxes.value)
      val project = matrix.finder().apply(sv)
      Def.task((project / key).value)
    }

  /**
   * When invoked on a ProjectMatrix with a TargetAxis, lookup the project
   * generated by `matrix` with a scalaVersion matching the one declared in that
   * TargetAxis, and resolve `key`.
   */
  def resolve[T](
      matrix: ProjectMatrix,
      key: SettingKey[T]
  ): Def.Initialize[T] =
    Def.settingDyn {
      val sv      = targetScalaVersion(virtualAxes.value)
      val project = matrix.finder().apply(sv)
      Def.setting((project / key).value)
    }

}
