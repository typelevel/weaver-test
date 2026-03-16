package weaver
package framework
package test

import org.typelevel.scalaccompat.annotation.unused
import cats.effect.IO

// snapshot4s adds a better development workflow for updating strings
// in tests.  It's only needed on a single platform (JVM) when developing
// locally.
//
// This file stubs out snapshot4s implementations for JS and
// native. The build doesn't depend on snapshot4s releases for
// these platforms.
object snapshot4s {
  class SnapshotConfig
  object generated {
    implicit val snapshotConfig: SnapshotConfig = new SnapshotConfig
  }
}

object SnapshotExpectations {
  def assertInlineSnapshot[A](found: A, snapshot: A)(
      implicit @unused config: snapshot4s.SnapshotConfig,
      comparison: Comparison[A]
  ): IO[Expectations] =
    IO(Expectations.Helpers.expect.eql(found, snapshot))
}
