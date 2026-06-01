import cats.effect.IO

package object weaver {

  type GlobalResource = IOGlobalResource
  type GlobalRead     = GlobalResourceF.Read[IO]
  type GlobalWrite    = GlobalResourceF.Write[IO]
}
