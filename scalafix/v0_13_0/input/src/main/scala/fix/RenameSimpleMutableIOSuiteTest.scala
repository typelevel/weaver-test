/*
rule = v0_13_0
 */
package fix

import weaver.SimpleMutableIOSuite
import cats.effect._

object BasicSimpleMutableIOSuite extends SimpleMutableIOSuite {
  def basicFunction(suite: SimpleMutableIOSuite): SimpleMutableIOSuite = suite
}

import weaver.{ SimpleMutableIOSuite => SimpleMutableIOSuiteAlias }

object AnotherSimpleMutableIOSuite extends SimpleMutableIOSuiteAlias
