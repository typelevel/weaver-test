package weaver
package framework
package test

import cats.kernel.Eq

object AssertionsTests extends SimpleIOSuite {

  pureTest("and") {
    assert(1 == 1)
    assert(2 == 2)
    success
  }

  pureTest("all") {
    assert.all(
      1 == 1,
      "a" + "b" == "ab",
      true || false
    )
    success
  }

  pureTest("equality check") {
    assert.same("foo", "foo")
  }

  pureTest("assert.eql respects cats.kernel.Eq") {
    implicit val eqInt: Eq[Int] = Eq.allEqual
    assert.eql(0, 1)
    success
  }

  pureTest("assert.eql respects weaver.Comparison") {
    implicit val comparison: Comparison[Int] = Comparison.fromEq(Eq.allEqual)
    assert.eql(0, 1)
    success
  }
}
