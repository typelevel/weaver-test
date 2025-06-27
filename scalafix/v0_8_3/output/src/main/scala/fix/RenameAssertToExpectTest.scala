package fix
import weaver._

object RenameAssertToExpectTest extends SimpleIOSuite {

  pureTest("and") {
    expect(1 == 1) and expect(2 == 2) && not(expect(1 == 2))
  }

  pureTest("or") {
    (expect(1 == 1) or expect(2 == 1)) &&
    (expect(2 == 1) or expect(1 == 1)) &&
    not(expect(2 == 1) || expect(1 == 2))
  }

  pureTest("xor") {
    (expect(1 == 1) xor expect(2 == 1)) &&
    (expect(2 == 1) xor expect(1 == 1)) &&
    not(expect(1 == 1) xor expect(2 == 2)) &&
    not(expect(2 == 1) xor expect(1 == 2))
  }
}
