/*
rule = RenameExpectToExpectSame
 */
package fix
import weaver._

object RenameExpectToExpectSameTest extends SimpleIOSuite {

  pureTest("==") {
    expect(1 == 1) and expect(2 == 2) && not(expect(1 == 2))
  }

  pureTest("not ==") {
    expect(1 > 0)
  }

  pureTest("nested ==") {
    expect(1 == 1 && 2 == 2) and expect(1 == 1 && 2 >= 2)
  }

}
