package weaver
package internals

import org.typelevel.scalaccompat.annotation.unused

private[weaver] trait MultiLineShowCompat {
  private[weaver] def productElementNames(
      @unused p: Product): Iterator[String] = Iterator.continually("")
  private[weaver] def collectionClassName(i: Iterable[_]): String =
    i.stringPrefix
}
