package fix

import weaver.FunSuite
import cats.effect._

object BasicFunSuiteIO extends FunSuite {
  def basicFunction(suite: FunSuite): FunSuite = suite
}

import weaver.{ FunSuite => FunSuiteIOAlias }

object AnotherFunSuiteIO extends FunSuiteIOAlias
