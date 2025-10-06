package weaver
package framework
package test

import cats.effect.IO
import cats.kernel.Eq

object ExpectationsTests extends SimpleIOSuite {

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

  pureTest("all") {
    expect.all(
      1 == 1,
      "a" + "b" == "ab",
      true || false
    )
  }

  pureTest("forall (success)") {
    forEach(List(true, true))(value => expect(value == true))
  }

  pureTest("forall (failure)") {
    not(forEach(List(true, false))(value => expect(value == true)))
  }

  pureTest("exists (success)") {
    exists(List("foo"))(s => expect.eql("foo", s))
  }

  pureTest("exists (failure)") {
    not(exists(List("foo"))(s => expect.eql("bar", s)))
  }

  pureTest("equality check") {
    expect.same("foo", "foo") and
      not(expect.same("bar", "foo"))
  }

  pureTest("matches pattern") {
    matches(Option(4)) { case Some(x) =>
      expect.eql(4, x)
    } and
      not(matches(Option(4)) { case None =>
        failure("dead code")
      })
  }

  test("matchOrFailFast (success)") {
    matchOrFailFast[IO](Some(4)) {
      case Some(v) => v
    }.as(success)
  }

  test("matchOrFailFast (failure)") {
    matchOrFailFast[IO](Option.empty[Int]) {
      case Some(v) => v
    }
      .attempt
      .map { either =>
        matches(either) { case Left(_: ExpectationFailed) =>
          success
        }
      }
  }

  pureTest("expect.eql respects cats.kernel.Eq") {
    implicit val eqInt: Eq[Int] = Eq.allEqual
    expect.eql(0, 1)
  }

  pureTest("expect.eql respects weaver.Comparison") {
    implicit val comparison: Comparison[Int] = Comparison.fromEq(Eq.allEqual)
    expect.eql(0, 1)
  }

  pureTest("when success") {
    val good: Either[String, Int] =
      Right(4)

    val bad: Either[String, Int] =
      Left("bad")

    whenSuccess(good)(expect.eql(4, _)) and
      not(whenSuccess(bad)(_ =>
        failure("unexpected run of success handler given failure payload")))
  }

}
