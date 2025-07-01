package fix
import weaver._

object AddClueToExpectTest extends SimpleIOSuite {

  val a: Int = 1
  val b: Int = 2
  val c: Int = 3

  pureTest("multiple") {
    expect(clue(a) == clue(b)) and expect(clue(b) == clue(c)) && not(expect(clue(c) == clue(a)))
  }

  pureTest("infix") {
    expect(clue(b) > clue(a))
  }

  pureTest("apply") {
    expect(clue(Some(2)).nonEmpty)
  }

  pureTest("apply(2)") {
    def isGreater(a: Int, b: Int, c: Int): Boolean = a > b && b > c
    expect(isGreater(clue(a), clue(b), clue(c)))
  }

  pureTest("apply(3)") {
    def isGreater(a: Int, b: Int)(c: Int): Boolean = a > b && b > c
    expect(isGreater(clue(a), clue(b))(clue(c)))
  }

  pureTest("ignore anonymous functions") {
    expect(clue(Some(1)).fold(true)(_ == 1))
  }

  pureTest("ignore functions") {
    expect(clue(Some(1)).fold(true)(x => x == 1))
  }

  pureTest("ignore literals") {
    expect(1 == 1)
  }

  pureTest("ignore clue") {
    expect(clue(1) > 0)
  }
}
