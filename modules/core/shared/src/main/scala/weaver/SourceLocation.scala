package weaver

// kudos to https://github.com/monix/minitest
class SourceLocation private (
    private[weaver] val filePath: String,
    private[weaver] val fileRelativePath: String,
    private[weaver] val line: Int
) {
  private[weaver] def fileName: Option[String] = filePath.split("/").lastOption
}

object SourceLocation extends SourceLocationMacro {
  def apply(
      filePath: String,
      fileRelativePath: String,
      line: Int
  ): SourceLocation = new SourceLocation(
    filePath,
    fileRelativePath,
    line
  )
}
