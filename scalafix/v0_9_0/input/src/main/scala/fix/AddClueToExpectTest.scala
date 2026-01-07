/*
rule = AddClueToExpect
 */
package fix
import weaver._

object AddClueToExpectTest extends SimpleIOSuite {

  val a: Int = 1
  val b: Int = 2
  val c: Int = 3

  pureTest("multiple") {
    expect(a == b) and expect(b == c) && not(expect(c == a))
  }

  pureTest("infix") {
    expect(b > a)
  }

  pureTest("apply") {
    expect(Some(2).nonEmpty)
  }

  pureTest("apply(2)") {
    def isGreater(a: Int, b: Int, c: Int): Boolean = a > b && b > c
    expect(isGreater(a, b, c))
  }

  pureTest("apply(3)") {
    def isGreater(a: Int, b: Int)(c: Int): Boolean = a > b && b > c
    expect(isGreater(a, b)(c))
  }

  pureTest("negation") {
    expect(!a.<(b))
  }

  pureTest("type application") {
    expect(a.isInstanceOf[Int])
  }

  pureTest("negated type application") {
    expect(!a.isInstanceOf[Int])
  }

  pureTest("select") {
    val either = Left[Int, Int](1)
    expect(either.toOption.isEmpty)
  }

  pureTest("ignore anonymous functions") {
    expect(Some(1).fold(true)(_ == 1))
  }

  pureTest("ignore functions") {
    expect(Some(1).fold(true)(x => x == 1))
  }

  pureTest("ignore literals") {
    expect(1 == 1)
  }

  pureTest("ignore clue") {
    expect(clue(1) > 0)
  }

  pureTest("all") {
    def isGreater(a: Int, b: Int, c: Int): Boolean = a > b && b > c
    expect.all(a == b, Some(2).nonEmpty, isGreater(a, b, c))
  }

  pureTest("ignore clue in expect.all") {
    expect.all(clue(1) > 0)
  }

  pureTest("ignore named parameters") {
    expect(Some(1).fold(ifEmpty = true)(_ => false))
  }

  pureTest("ignore blocks") {
    expect(Some(1).fold(true)({ _ => b == c }))
  }

}
