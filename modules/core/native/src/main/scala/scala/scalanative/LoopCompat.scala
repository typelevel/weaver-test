package scala.scalanative

// Exposes the package-private replacement for the deprecated scalanative.runtime.loop().
// Must live in package scala.scalanative to access NativeExecutionContext.queueInternal which is private[scalanative]
// as suggested by deprecation warning
object LoopCompat {
  def helpComplete(): Unit =
    concurrent.NativeExecutionContext.queueInternal.helpComplete()
}
