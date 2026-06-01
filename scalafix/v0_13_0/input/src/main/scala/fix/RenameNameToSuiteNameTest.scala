/*
rule = v0_13_0
 */
package fix
import weaver._

object RenameNameToSuiteName extends SimpleIOSuite {

  pureTest("basic") {
    expect.same(name, "basic")
  }

  pureTest("other name") {
    val name = "basic"
    expect.same(name, "basic")
  }

  def getName(suite: SimpleIOSuite): String = {
    suite.name
  }
}
