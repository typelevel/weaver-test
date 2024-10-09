package weaver

import internals._
import cats.data.Validated

class Assert extends ExpectSame with AssertMacro

object Assert {
  private[weaver] def throwWhenFailed(expectations: Expectations): Unit = {
    expectations.run match {
      case Validated.Invalid(errs) => throw errs.head
      case Validated.Valid(())     => ()
    }
  }
}
