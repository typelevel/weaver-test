package weaver

// kudos to https://github.com/monix/minitest
class SourceLocation private (
    private[weaver] val filePath: String,
    private[weaver] val fileRelativePath: String,
    private[weaver] val line: Int,
    private[weaver] val sourceCode: Option[SourceLocation.SourceCode]
) {
  private[weaver] def fileName: Option[String] = filePath.split("/").lastOption
}

object SourceLocation extends SourceLocationMacro {

  private[weaver] class SourceCode(
      private[weaver] val sourceLine: String,
      private[weaver] val column: Int
  )

  def apply(
      filePath: String,
      fileRelativePath: String,
      line: Int,
      sourceLineAndColumn: Option[(String, Int)]
  ): SourceLocation = new SourceLocation(
    filePath,
    fileRelativePath,
    line,
    sourceLineAndColumn.map { case (source, col) =>
      new SourceCode(source, col)
    }
  )
}
