/*
rule = RewriteExpect
 */
package fix
import weaver._
import cats.syntax.all._

object RewriteExpectTest extends SimpleIOSuite {

  pureTest("==") {
    expect(1 == 1) and expect(2 == 2) && not(expect(1 == 2))
  }

  pureTest("not ==") {
    expect(1 > 0)
  }

  pureTest("nested ==") {
    expect(1 == 1 && 2 == 2) and expect(1 == 1 && 2 >= 2)
  }

  pureTest("===") {
    expect(1 === 1 && 2.===(2))
  }

  pureTest("eqv") {
    expect(1.eqv(1) && (2 eqv 2))
  }

  pureTest("ignore clue") {
    expect(clue(1) == 2)
  }

  pureTest("|| and &&") {
    expect(1 == 2 && 3 > 4 || 4 > 4 && 5 < 5)
  }

  pureTest("if then else") {
    expect(if (1 == 2) 3 > 4 else 5 == 5)
  }

  pureTest("if then else if") {
    expect(if (1 == 2) 3 > 4 else if (2 == 3) 5 == 5 else 4 > 3)
  }

  pureTest("match") {
    val n: Int = 1
    expect(n match {
      case 2 => 1 > 2
      case 3 => true
      case 4 => false
    })
  }

  pureTest("match with wildcard false") {
    val n: Int = 1
    expect(n match {
      case 2 => 1 > 2
      case _ => false
    })
  }

  pureTest("expect.all ==") {
    expect.all(1 == 1, 2 == 2, 3 == 3)
  }

  pureTest("expect.all ===") {
    expect.all(1 === 1, 2 == 2, 3 == 3)
  }

  pureTest("expect.all with one not ==") {
    expect.all(1 > 0, 2 == 2)
  }

  pureTest("expect.all with some not ==") {
    expect.all(1 > 0, 2 > 0, 2 == 2)
  }

  pureTest("expect.all with || and &&") {
    expect.all(1 > 0 && 2 == 2, 2 > 0 || 3 > 4, 2 == 2)
  }

  pureTest("expect.all ignore clue") {
    expect.all(1 == 1, clue(2) == 2, 3 === clue(3))
  }

  pureTest("infer order with literals") {
    val result = 2
    expect(result == 1)
  }

  pureTest("infer order with constructors") {
    val result = List(2)
    expect(result == List(1))
  }

  pureTest("infer order with sealed traits") {
    sealed trait Pet
    object Pet {
      case object Cat             extends Pet
      case class Dog(friend: Pet) extends Pet
    }
    val petCat = Pet.Cat
    val petDog = Pet.Dog(Pet.Cat)
    expect(petCat == Pet.Cat && petDog == Pet.Dog(Pet.Cat))
  }

  pureTest("infer order with common names") {
    val expectedId = 1
    val actualId   = 2
    val obtainedId = 3
    val result     = 4
    expect(
      result == expectedId && actualId == expectedId && obtainedId == expectedId)
  }
}
