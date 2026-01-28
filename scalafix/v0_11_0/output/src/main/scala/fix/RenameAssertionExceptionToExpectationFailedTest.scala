package fix
import weaver._
import cats.data.NonEmptyList
import cats.effect.IO

object RenameAssertionExceptionToExpectationFailed extends SimpleIOSuite {

  test("raise") {
    IO.raiseError(new ExpectationFailed(
      "some reason",
      NonEmptyList.of(implicitly[SourceLocation]))).as(success)
  }

  test("match") {
    val program: IO[Unit] = IO.unit
    program.attemptTap {
      case Left(ex: ExpectationFailed) => IO.println(ex)
      case _                           => IO.unit
    }.as(success)
  }

  test("import") {
    import weaver.ExpectationFailed
    IO.raiseError(new ExpectationFailed(
      "some reason",
      NonEmptyList.of(implicitly[SourceLocation]))).as(success)
  }
}
