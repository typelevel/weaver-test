package fix
import weaver._

object RenameNameToSuiteName extends SimpleIOSuite {

  pureTest("basic") {
    expect.same(suiteName, "basic")
  }

  pureTest("other name") {
    val name = "basic"
    expect.same(name, "basic")
  }

  def getName(suite: SimpleIOSuite): String = {
    suite.suiteName
  }
}
