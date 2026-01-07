package fix
import weaver._

object AddClueToExpectTest extends SimpleIOSuite {

  val a: Int = 1
  val b: Int = 2
  val c: Int = 3

  pureTest("multiple") {
    expect(clue(a) == clue(b)) and expect(clue(b) == clue(c)) && not(
      expect(clue(c) == clue(a)))
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

  pureTest("negation") {
    expect(!clue(a).<(clue(b)))
  }

  pureTest("type application") {
    expect(clue(a).isInstanceOf[Int])
  }

  pureTest("negated type application") {
    expect(!clue(a).isInstanceOf[Int])
  }

  pureTest("select") {
    val either = Left[Int, Int](1)
    expect(clue(either.toOption).isEmpty)
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

  pureTest("all") {
    def isGreater(a: Int, b: Int, c: Int): Boolean = a > b && b > c
    expect.all(clue(a) == clue(b),
               clue(Some(2)).nonEmpty,
               isGreater(clue(a), clue(b), clue(c)))
  }

  pureTest("ignore clue in expect.all") {
    expect.all(clue(1) > 0)
  }

  pureTest("ignore named parameters") {
    expect(clue(Some(1)).fold(ifEmpty = true)(_ => false))
  }

  pureTest("ignore blocks") {
    expect(clue(Some(1)).fold(true) {
      _ => b == c
    })
  }

}
