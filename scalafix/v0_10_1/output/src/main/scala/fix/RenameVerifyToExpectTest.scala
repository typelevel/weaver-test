package fix
import weaver._

object RenameVerifyToExpectTest extends SimpleIOSuite {

  pureTest("basic") {
    expect(1 == 1)
  }

  pureTest("message") {
    expect(1 == 1, "failure")
  }
}
