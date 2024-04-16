package weaver.diff

class Clue[+T](
    val source: String,
    val value: T,
    val valueType: String
) extends Serializable {
  override def toString(): String = s"Clue($source, $value)"
}

object Clue {
  def fromValue[T](value: T): Clue[T] = new Clue("", value, "")
}
