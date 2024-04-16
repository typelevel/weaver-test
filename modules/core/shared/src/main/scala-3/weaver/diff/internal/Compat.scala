package munit.diff.internal

import scala.reflect.Selectable.reflectiveSelectable

object Compat {
  type LazyList[+T] = scala.LazyList[T]
  val LazyList = scala.LazyList
  def productElementNames(p: Product): Iterator[String] =
    p.productElementNames
  def collectionClassName(i: Iterable[_]): String = {
    i.asInstanceOf[{ def collectionClassName: String }].collectionClassName
  }
}
