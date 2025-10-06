/*
rule = RenameCancelToIgnore
 */
package fix
import weaver._

object RenameCancelToIgnore extends SimpleIOSuite {

  test("basic") {
    cancel("some reason").as(success)
  }
  test("with source position") {
    cancel("some reason")(implicitly[SourceLocation]).as(success)
  }
}
