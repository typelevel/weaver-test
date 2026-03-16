package scala.scalanative

// Exposes the package-private replacement for the deprecated scalanative.runtime.loop().
// Must live in package scala.scalanative to access NativeExecutionContext.queueInternal which is private[scalanative]
// as suggested by deprecation warning: https://github.com/scala-native/scala-native/blob/d696e695268146edce1a96e51188218a31cc782e/nativelib/src/main/scala/scala/scalanative/runtime/package.scala#L169
object LoopCompat {
  def helpComplete(): Unit =
    concurrent.NativeExecutionContext.queueInternal.helpComplete()
}
