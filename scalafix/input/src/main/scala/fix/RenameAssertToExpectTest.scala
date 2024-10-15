/*
rule = RenameAssertToExpect
*/
package fix
import weaver._

object RenameAssertToExpectTest extends SimpleIOSuite {

  pureTest("and") {
    assert(1 == 1) and assert(2 == 2) && not(assert(1 == 2))
  }

  pureTest("or") {
    (assert(1 == 1) or assert(2 == 1)) &&
    (assert(2 == 1) or assert(1 == 1)) &&
    not(assert(2 == 1) || assert(1 == 2))
  }

  pureTest("xor") {
    (assert(1 == 1) xor assert(2 == 1)) &&
    (assert(2 == 1) xor assert(1 == 1)) &&
    not(assert(1 == 1) xor assert(2 == 2)) &&
    not(assert(2 == 1) xor assert(1 == 2))
  }
}
