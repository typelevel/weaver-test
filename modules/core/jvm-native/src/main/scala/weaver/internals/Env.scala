package weaver.internals

private[weaver] object Env {
  def get(key: String): Option[String] = sys.env.get(key)
}
