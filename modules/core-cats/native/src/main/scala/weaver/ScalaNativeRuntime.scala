package scala.scalanative

object ScalaNativeRuntime {
  def run() =
    scala.scalanative.concurrent.NativeExecutionContext.queueInternal.helpComplete()
}
