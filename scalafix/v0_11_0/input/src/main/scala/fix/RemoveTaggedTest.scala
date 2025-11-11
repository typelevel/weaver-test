/*
rule = v0_11_0
 */
package fix
import weaver._

object RemoveTaggedTest extends SimpleIOSuite {

  pureTest("ignore-test".tagged("ignore")) {
    success
  }

  pureTest("only-test".tagged("only")) {
    success
  }

  pureTest("other-tag-test".tagged("other")) {
    success
  }
}
