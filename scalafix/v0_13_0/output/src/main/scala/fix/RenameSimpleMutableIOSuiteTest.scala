package fix

import weaver.SimpleIOSuite
import cats.effect._

object BasicSimpleMutableIOSuite extends SimpleIOSuite {
  def basicFunction(suite: SimpleIOSuite): SimpleIOSuite = suite
}

import weaver.{ SimpleIOSuite => SimpleMutableIOSuiteAlias }

object AnotherSimpleMutableIOSuite extends SimpleMutableIOSuiteAlias
