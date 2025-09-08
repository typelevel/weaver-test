package scala.scalanative.concurrent.weaver.internals

object NativeExecutionContext {
  def helpComplete(): Unit =
    scala.scalanative.concurrent.NativeExecutionContext.queueInternal.helpComplete()
}
