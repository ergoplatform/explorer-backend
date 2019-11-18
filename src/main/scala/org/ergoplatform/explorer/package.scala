package org.ergoplatform

import doobie.util.{Get, Put}
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe._
import io.estatico.newtype.NewType

package object explorer {

  type Id = Id.Type

  object Id extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = deriving
    implicit def put: Put[Type] = deriving

    // circe instances
    implicit def encoder: Encoder[Type] = deriving
    implicit def decoder: Decoder[Type] = deriving
  }

  type TxId = TxId.Type

  object TxId extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = deriving
    implicit def put: Put[Type] = deriving

    // circe instances
    implicit def encoder: Encoder[Type] = deriving
    implicit def decoder: Decoder[Type] = deriving
  }

  type BoxId = BoxId.Type

  object BoxId extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = deriving
    implicit def put: Put[Type] = deriving

    // circe instances
    implicit def encoder: Encoder[Type] = deriving
    implicit def decoder: Decoder[Type] = deriving
  }

  type AssetId = AssetId.Type

  object AssetId extends NewType.Default[String] {
    // doobie instances
    implicit def get: Get[Type] = deriving
    implicit def put: Put[Type] = deriving

    // circe instances
    implicit def encoder: Encoder[Type] = deriving
    implicit def decoder: Decoder[Type] = deriving
  }

  type Address = String Refined Base58Spec

  type HexString = String Refined HexStringSpec

  type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]
}
