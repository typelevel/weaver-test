package fix
import weaver._

object RenameExpectToExpectSameTest extends SimpleIOSuite {

  pureTest("==") {
    expect.same(1, 1) and expect.same(2, 2) && not(expect.same(1, 2))
  }

  pureTest("not ==") {
    expect(1 > 0)
  }

  pureTest("nested ==") {
    expect(1 == 1 && 2 == 2) and expect(1 == 1 && 2 >= 2)
  }

}
