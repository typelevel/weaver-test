/*
rule = v0_11_0
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
