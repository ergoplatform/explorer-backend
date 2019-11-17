package org.ergoplatform

import doobie.util.{Get, Put}
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.estatico.newtype.NewType
import io.estatico.newtype.ops._

package object explorer {

  type Id = Id.Type

  object Id extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = Get[String].map(_.coerce)
    implicit def put: Put[Type] = Put[String].contramap(unwrap(_))
  }

  type TxId = TxId.Type

  object TxId extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = Get[String].map(_.coerce)
    implicit def put: Put[Type] = Put[String].contramap(unwrap(_))
  }

  type BoxId = BoxId.Type

  object BoxId extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = Get[String].map(_.coerce)
    implicit def put: Put[Type] = Put[String].contramap(unwrap(_))
  }

  type AssetId = AssetId.Type

  object AssetId extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = Get[String].map(_.coerce)
    implicit def put: Put[Type] = Put[String].contramap(unwrap(_))
  }

  type Address = String Refined Base58Spec

  type HexString = String Refined HexStringSpec

  type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]
}
