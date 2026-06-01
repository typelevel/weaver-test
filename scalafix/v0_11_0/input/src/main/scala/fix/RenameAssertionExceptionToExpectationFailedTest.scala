/*
rule = v0_11_0
 */
package fix
import weaver._
import cats.data.NonEmptyList
import cats.effect.IO

object RenameAssertionExceptionToExpectationFailed extends SimpleIOSuite {

  test("raise") {
    IO.raiseError(AssertionException("some reason", NonEmptyList.of(implicitly[SourceLocation]))).as(success)
  }

  test("match") {
    val program: IO[Unit] = IO.unit
    program.attemptTap {
      case Left(ex: AssertionException) => IO.println(ex)
      case _ => IO.unit
    }.as(success)
  }

  test("import") {
    import weaver.AssertionException
    IO.raiseError(AssertionException("some reason", NonEmptyList.of(implicitly[SourceLocation]))).as(success)
  }
}
