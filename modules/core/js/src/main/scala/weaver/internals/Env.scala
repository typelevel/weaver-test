package weaver.internals

import scala.scalajs.js
import scala.util.Try

private[weaver] object Env {
  def get(name: String): Option[String] = processEnv.get(name).collect {
    case value: String => value
  }

  // Attempt to read NodeJS environment variables
  private def processEnv: js.Dictionary[Any] =
    Try(js.Dynamic.global.process.env.asInstanceOf[js.Dictionary[Any]])
      .getOrElse(js.Dictionary.empty)

}
