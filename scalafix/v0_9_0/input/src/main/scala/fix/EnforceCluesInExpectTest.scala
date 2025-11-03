/*
rule = EnforceCluesInExpect
 */
package fix
import weaver._
import cats.syntax.all._

object EnforceCluesInExpect extends SimpleIOSuite {

  val a: Int = 1
  val b: Int = 2
  val c: Int = 3

  pureTest("expect") {
    expect(a < b) // assert: EnforceCluesInExpect
  }

  pureTest("expect ==") {
    expect(a == b) // assert: EnforceCluesInExpect
  }

  pureTest("expect ===") {
    expect(a === b) // assert: EnforceCluesInExpect
  }

  pureTest("expect message") {
    expect(a < b) /* assert: EnforceCluesInExpect
           ^^^^^
    assertion must contain clues. Read https://typelevel.org/weaver-test/features/troubleshooting_failures.html#using-clue-with-expect for more details.
    */
  }

  pureTest("multiple expect") {
    expect(a == b) and expect(b == c) && not(expect(c === a)) // assert: EnforceCluesInExpect
  }

  pureTest("message ==") {
    expect(a == b) /* assert: EnforceCluesInExpect
           ^^^^^^
    equality assertion should use `expect.eql` or `expect.same`. Read https://typelevel.org/weaver-test/features/asserting_equality.html for more details.
    */
  }

  pureTest("message ===") {
    expect(a === b) /* assert: EnforceCluesInExpect
           ^^^^^^^
    equality assertion should use `expect.eql`. Read https://typelevel.org/weaver-test/features/asserting_equality.html for more details.
    */
  }

  pureTest("expect with clue in expectation") {
    expect(clue(1) > 0)
  }

  pureTest("expect with boolean expression") {
    val isEqual = a == b
    expect(isEqual)
  }

  pureTest("expect with negated boolean expression") {
    val isEqual = a == b
    expect(!isEqual)
  }

  pureTest("expect.all") {
    expect.all(a == b, b == c) // assert: EnforceCluesInExpect
  }

  pureTest("expect.all message") {
    expect.all(a > b) /* assert: EnforceCluesInExpect
               ^^^^^
    assertion must contain clues. Read https://typelevel.org/weaver-test/features/troubleshooting_failures.html#using-clue-with-expect for more details.
    */
  }

  pureTest("expect.all equality message") {
    expect.all(a == b) /* assert: EnforceCluesInExpect
               ^^^^^^
    equality assertion should use `expect.eql` or `expect.same`. Read https://typelevel.org/weaver-test/features/asserting_equality.html for more details.
    */
  }


  pureTest("expect.all with clues in some expectations") {
    expect.all(a == clue(b), b == c) // assert: EnforceCluesInExpect
  }

  pureTest("expect.all with clues in every expectation") {
    expect.all(a == clue(b), b == clue(c))
  }
}
