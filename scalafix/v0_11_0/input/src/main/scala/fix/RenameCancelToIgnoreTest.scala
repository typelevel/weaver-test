/*
rule = v0_11_0
 */
package fix
import weaver._

object RenameCancelToIgnore extends SimpleIOSuite {

  test("basic") {
    cancel("some reason").as(success)
  }
}
