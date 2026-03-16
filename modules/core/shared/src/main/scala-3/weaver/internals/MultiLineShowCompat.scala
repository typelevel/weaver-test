package weaver
package internals

import scala.reflect.Selectable.reflectiveSelectable

private[weaver] trait MultiLineShowCompat {
  private[weaver] def productElementNames(p: Product): Iterator[String] =
    p.productElementNames
  private[weaver] def collectionClassName(i: Iterable[_]): String = i
    .asInstanceOf[{ def collectionClassName: String }].collectionClassName

}
