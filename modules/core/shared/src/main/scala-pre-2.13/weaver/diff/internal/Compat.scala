package weaver.diff.internal

object Compat {
  type LazyList[+T] = Stream[T]
  val LazyList = scala.Stream
  def productElementNames(p: Product): Iterator[String] =
    Iterator.continually("")
  def collectionClassName(i: Iterable[_]): String =
    i.stringPrefix
}
