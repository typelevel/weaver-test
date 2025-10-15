package weaver

import java.util.regex.Pattern
import weaver.internals.TagExprParser

private[weaver] object Filters {

  private[weaver] def toPattern(filter: String): Pattern = {
    val parts = filter
      .split("\\*", -1)
      .map { // Don't discard trailing empty string, if any.
        case ""  => ""
        case str => Pattern.quote(str)
      }
    Pattern.compile(parts.mkString(".*"))
  }

  private type Predicate = TestName => Boolean

  private object atLine {
    def unapply(testPath: String): Option[(String, Int)] = {
      // Can't use string interpolation in pattern (2.12)
      val members = testPath.split(".line://")
      if (members.size == 2) {
        val suiteName = members(0)
        // Can't use .toIntOption (2.12)
        val maybeLine = scala.util.Try(members(1).toInt).toOption
        maybeLine.map(suiteName -> _)
      } else None
    }
  }

  private def createTagFilter(expr: String): TestName => Boolean = {
    TagExprParser.parse(expr) match {
      case Right(tagExpr) =>
        testName => tagExpr.eval(testName.tags)
      case Left(error) =>
        throw new IllegalArgumentException(
          s"Invalid tag expression '$expr': $error"
        )
    }
  }

  private[weaver] def filterTests(suiteName: String)(
      args: List[String]): TestName => Boolean = {

    def toPredicate(filter: String): Predicate = {
      filter match {

        case atLine(`suiteName`, line) => {
          case TestName(_, indicator, _) => indicator.line == line
        }
        case regexStr => {
          case TestName(name, _, _) =>
            val fullName = suiteName + "." + name
            toPattern(regexStr).matcher(fullName).matches()
        }
      }
    }

    import scala.util.Try
    def indexOfOption(opt: String): Option[Int] =
      Option(args.indexOf(opt)).filter(_ >= 0)

    // Tag-based filtering
    val maybeTagFilter = for {
      index <- indexOfOption("-t").orElse(indexOfOption("--tags"))
      expr  <- Try(args(index + 1)).toOption
    } yield createTagFilter(expr)

    // Keep existing pattern-based filtering for backwards compatibility
    val maybePatternFilter = for {
      index  <- indexOfOption("-o").orElse(indexOfOption("--only"))
      filter <- Try(args(index + 1)).toOption
    } yield toPredicate(filter)

    testName => {
      maybeTagFilter.forall(_.apply(testName)) &&
      maybePatternFilter.forall(_.apply(testName))
    }
  }
}
