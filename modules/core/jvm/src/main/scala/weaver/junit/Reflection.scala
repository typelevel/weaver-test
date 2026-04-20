package weaver
package junit

import scala.util.control.NoStackTrace

import org.portablescala.reflect.Reflect

private object Reflection {

  private[junit] type AnyEffect[A] = Any

  private[junit] def loadRunnableSuite[A](
      qualifiedName: String,
      loader: ClassLoader): RunnableSuite[AnyEffect] =
    Reflect.lookupLoadableModuleClass(qualifiedName, loader) match {
      case Some(cls) => cls.loadModule().asInstanceOf[RunnableSuite[AnyEffect]]
      case None      =>
        Reflect.lookupInstantiatableClass(qualifiedName, loader) match {
          case None =>
            throw new Exception(s"Could not find class $qualifiedName")
              with NoStackTrace
          case Some(cls) =>
            cls.getConstructor(classOf[GlobalResourceF.Read[AnyEffect]]) match {
              case Some(value) =>
                // Instantiating with null a first time to retrieve the effect...
                val unused =
                  value.newInstance(null).asInstanceOf[RunnableSuite[AnyEffect]]
                val effectCompat = unused.getEffectCompat
                val read = GlobalResourceF.Read.empty(effectCompat.effect)
                // Re-instantiating with empty global read.
                value.newInstance(read).asInstanceOf[RunnableSuite[AnyEffect]]
              case None =>
                throw new Exception("Could not find a suitable constructor that takes GlobalResourceF.Read")
            }
        }

    }
}
