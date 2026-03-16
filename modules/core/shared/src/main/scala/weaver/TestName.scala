package weaver

/**
 * An identifier to a test in a suite.
 *
 * The implicit conversion from String is used as a mean for IDEs to detect the
 * location of individual tests.
 */
case class TestName(
    name: String,
    location: SourceLocation,
    tags: Set[TestName.Tag]) {
  def only: TestName   = this.copy(tags = tags + TestName.Tag.Only)
  def ignore: TestName = this.copy(tags = tags + TestName.Tag.Ignore)
}

object TestName {
  implicit def fromString(s: String)(
      implicit location: SourceLocation): TestName =
    TestName(s, location, Set.empty)

  sealed trait Tag
  private[weaver] object Tag {
    private[weaver] case object Only   extends Tag
    private[weaver] case object Ignore extends Tag
  }
}
