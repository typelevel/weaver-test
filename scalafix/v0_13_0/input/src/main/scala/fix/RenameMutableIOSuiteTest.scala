/*
rule = v0_13_0
 */
package fix

import weaver.MutableIOSuite
import cats.effect._

object BasicMutableIOSuite extends MutableIOSuite {

  type Res = Unit
  def sharedResource: Resource[IO, Res] = Resource.unit

  def basicFunction(suite: MutableIOSuite): MutableIOSuite = suite
}

import weaver.{ MutableIOSuite => MutableIOSuiteAlias }

object AnotherMutableIOSuite extends MutableIOSuiteAlias {
  type Res = Unit
  def sharedResource: Resource[IO, Res] = Resource.unit
}

import weaver.MutableFSuite

trait BasicMutableFSuite extends MutableFSuite[IO]
