package weaver.diff

import weaver.diff.console.Printers
import weaver.diff.console.AnsiColors

import scala.jdk.CollectionConverters._

class Diff(val obtained: String, val expected: String) extends Serializable {
  val obtainedClean: String      = AnsiColors.filterAnsi(obtained)
  val expectedClean: String      = AnsiColors.filterAnsi(expected)
  val obtainedLines: Seq[String] = splitIntoLines(obtainedClean)
  val expectedLines: Seq[String] = splitIntoLines(expectedClean)
  val unifiedDiff: String = createUnifiedDiff(obtainedLines, expectedLines)

  def createReport(
      title: String,
      printObtainedAsStripMargin: Boolean = true
  ): String = {
    val sb = new StringBuilder
    if (title.nonEmpty) {
      sb.append(title)
        .append("\n")
    }
    if (obtainedClean.length < 1000) {
      header("Obtained", sb).append("\n")
      if (printObtainedAsStripMargin) {
        sb.append(asStripMargin(obtainedClean))
      } else {
        sb.append(obtainedClean)
      }
      sb.append("\n")
    }
    appendDiffOnlyReport(sb)
    sb.toString()
  }

  private def appendDiffOnlyReport(sb: StringBuilder): Unit = {
    header("Diff", sb)
    val red   = AnsiColors.use(AnsiColors.LightRed)
    val reset = AnsiColors.use(AnsiColors.Reset)
    val green = AnsiColors.use(AnsiColors.LightGreen)
    sb.append(s" (${red}- obtained${reset}, ${green}+ expected${reset})")
    sb.append("\n")
    sb.append(unifiedDiff)
  }

  private def asStripMargin(obtained: String): String = {
    if (!obtained.contains("\n")) Printers.print(obtained)
    else {
      val out   = new StringBuilder
      val lines = obtained.trim.linesIterator
      val head  = if (lines.hasNext) lines.next() else ""
      out.append("    \"\"\"|" + head + "\n")
      lines.foreach(line => {
        out.append("       |").append(line).append("\n")
      })
      out.append("       |\"\"\".stripMargin")
      out.toString()
    }
  }

  private def header(t: String, sb: StringBuilder): StringBuilder = {
    sb.append(AnsiColors.c(s"=> $t", AnsiColors.Bold))
  }

  private def createUnifiedDiff(
      original: Seq[String],
      revised: Seq[String]
  ): String = {
    val diff = DiffUtils.diff(original.asJava, revised.asJava)
    val result =
      if (diff.getDeltas.isEmpty) ""
      else {
        DiffUtils
          .generateUnifiedDiff(
            "obtained",
            "expected",
            original.asJava,
            diff,
            1
          )
          .asScala
          .iterator
          .drop(2)
          .filterNot(_.startsWith("@@"))
          .map { line =>
            if (line.isEmpty()) line
            else if (line.last == ' ') line + "∙"
            else line
          }
          .map { line =>
            if (line.startsWith("-")) AnsiColors.c(line, AnsiColors.LightRed)
            else if (line.startsWith("+"))
              AnsiColors.c(line, AnsiColors.LightGreen)
            else line
          }
          .mkString("\n")
      }
    result
  }

  private def splitIntoLines(string: String): Seq[String] = {
    string.trim().replace("\r\n", "\n").split("\n").toIndexedSeq
  }
}
