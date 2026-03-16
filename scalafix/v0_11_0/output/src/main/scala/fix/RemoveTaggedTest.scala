package fix
import weaver._

object RemoveTaggedTest extends SimpleIOSuite {

  pureTest("ignore-test".ignore) {
    success
  }

  pureTest("only-test".only) {
    success
  }

  pureTest("other-tag-test") {
    success
  }
}
