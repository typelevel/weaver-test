/*
rule = v0_13_0
 */
package fix

import weaver.FunSuiteIO
import cats.effect._

object BasicFunSuiteIO extends FunSuiteIO {
  def basicFunction(suite: FunSuiteIO): FunSuiteIO = suite
}

import weaver.{ FunSuiteIO => FunSuiteIOAlias }

object AnotherFunSuiteIO extends FunSuiteIOAlias
