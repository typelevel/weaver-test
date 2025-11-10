/*
rule = RenameVerifyToExpect
 */
package fix
import weaver._

object RenameVerifyToExpectTest extends SimpleIOSuite {

  pureTest("basic") {
    verify(1 == 1)
  }

  pureTest("message") {
    verify(1 == 1, "failure")
  }
}
