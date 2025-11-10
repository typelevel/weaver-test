package fix
import weaver._

object RenameCancelToIgnore extends SimpleIOSuite {

  test("basic") {
    ignore("some reason").as(success)
  }
}
