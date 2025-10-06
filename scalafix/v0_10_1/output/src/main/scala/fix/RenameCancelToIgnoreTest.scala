package fix
import weaver._

object RenameCancelToIgnore extends SimpleIOSuite {

  test("basic") {
    ignore("some reason").as(success)
  }
  test("with source position") {
    ignore("some reason")(implicitly[SourceLocation]).as(success)
  }
}
