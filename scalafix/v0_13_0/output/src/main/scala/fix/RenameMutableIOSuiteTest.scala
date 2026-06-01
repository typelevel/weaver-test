package fix

import weaver.IOSuite
import cats.effect._

object BasicMutableIOSuite extends IOSuite {

  type Res = Unit
  def sharedResource: Resource[IO, Res] = Resource.unit

  def basicFunction(suite: IOSuite): IOSuite = suite
}

import weaver.{ IOSuite => MutableIOSuiteAlias }

object AnotherMutableIOSuite extends MutableIOSuiteAlias {
  type Res = Unit
  def sharedResource: Resource[IO, Res] = Resource.unit
}

import weaver.FSuite

trait BasicMutableFSuite extends FSuite[IO]
