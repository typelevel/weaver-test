package fix
import weaver._
import cats.syntax.all._

object RewriteExpectTest extends SimpleIOSuite {

  pureTest("==") {
    expect.same(1, 1) and expect.same(2, 2) && not(expect.same(1, 2))
  }

  pureTest("not ==") {
    expect(1 > 0)
  }

  pureTest("nested ==") {
    expect.same(1, 1).and(expect.same(2, 2)) and
      expect.same(1, 1).and(expect(2 >= 2))
  }

  pureTest("===") {
    expect.eql(1, 1).and(expect.eql(2, 2))
  }

  pureTest("eqv") {
    expect.eql(1, 1).and(expect.eql(2, 2))
  }

  pureTest("ignore clue") {
    expect(clue(1) == 2)
  }

  pureTest("|| and &&") {
    expect.same(1, 2).and(expect(3 > 4)).or(expect(4 > 4).and(expect(5 < 5)))
  }

  pureTest("if then else") {
    if (1 == 2) expect(3 > 4) else expect.same(5, 5)
  }

  pureTest("if then else if") {
    if (1 == 2) expect(3 > 4)
    else if (2 == 3) expect.same(5, 5) else expect(4 > 3)
  }

  pureTest("match") {
    val n: Int = 1
    n match {
      case 2 =>
        expect(1 > 2)
      case 3 =>
        success
      case 4 =>
        failure("Assertion failed")
    }
  }

  pureTest("match with wildcard false") {
    val n: Int = 1
    matches(n) {
      case 2 =>
        expect(1 > 2)
    }
  }

  pureTest("expect.all ==") {
    expect.same(1, 1).and(expect.same(2, 2)).and(expect.same(3, 3))
  }

  pureTest("expect.all ===") {
    expect.eql(1, 1).and(expect.same(2, 2)).and(expect.same(3, 3))
  }

  pureTest("expect.all with one not ==") {
    expect.same(2, 2).and(expect(1 > 0))
  }

  pureTest("expect.all with some not ==") {
    expect.same(2, 2).and(expect.all(1 > 0, 2 > 0))
  }

  pureTest("expect.all with || and &&") {
    expect(1 > 0).and(expect.same(2, 2)).and(expect(2 > 0).or(expect(3 >
      4))).and(expect.same(2, 2))
  }

  pureTest("expect.all ignore clue") {
    expect.same(1, 1).and(expect.all(clue(2) == 2, 3 === clue(3)))
  }

  pureTest("infer order with literals") {
    val result = 2
    expect.same(1, result)
  }

  pureTest("infer order with constructors") {
    val result = List(2)
    expect.same(List(1), result)
  }

  pureTest("infer order with sealed traits") {
    sealed trait Pet
    object Pet {
      case object Cat             extends Pet
      case class Dog(friend: Pet) extends Pet
    }
    val petCat = Pet.Cat
    val petDog = Pet.Dog(Pet.Cat)
    expect.same(Pet.Cat, petCat).and(expect.same(Pet.Dog(Pet.Cat), petDog))
  }

  pureTest("infer order with common names") {
    val expectedId = 1
    val actualId   = 2
    val obtainedId = 3
    val result     = 4
    expect.same(expectedId, result).and(expect.same(expectedId, actualId)).and(
      expect.same(expectedId, obtainedId))
  }
}
