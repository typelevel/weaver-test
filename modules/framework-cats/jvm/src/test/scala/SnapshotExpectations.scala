package weaver
package framework
package test

import cats.effect.IO

import snapshot4s.*

// Implements assertInlineSnapshot for weaver.
// See https://siriusxm.github.io/snapshot4s/contributing/supporting-a-test-framework/#integrating-your-own-test-framework
object SnapshotExpectations extends SnapshotAssertions[IO[Expectations]] {

  val nonExistentMessage: String =
    "Snapshot does not exist. Run 'sbt snapshot4sPromote' to create it."
  val failureMessage: String =
    "Snapshot not equal. Run 'sbt snapshot4sPromote' to update it."

  implicit def snapshotEqForCatsEq[A](implicit eq: cats.Eq[A]): SnapshotEq[A] =
    SnapshotEq.instance(eq.eqv)

  implicit def weaverResultLike[A](
      implicit loc: SourceLocation,
      cmp: Comparison[A]): ResultLike[A, IO[Expectations]] =
    new ResultLike[A, IO[Expectations]] {
      type Assertion = IO[Expectations]
      def apply(result: () => Result[A]): Assertion =
        IO(result()).map({
          case _: Result.Success[?] => Expectations.Helpers.success
          case _: Result.NonExistent[?] =>
            Expectations.Helpers.failure(nonExistentMessage)(loc)
          case Result.Failure(found, snapshot) =>
            Expectations.Helpers.failure(failureMessage)(
              loc).and(Expectations.Helpers.expect.eql(expected = snapshot,
                                                       found = found)(cmp, loc))
        })
    }
}
