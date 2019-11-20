package org.ergoplatform

import doobie.util.{Get, Put}
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe._
import io.estatico.newtype.macros.newtype

package object explorer {

  @newtype case class Id(value: String)

  object Id {
    // doobie instances
    implicit def get: Get[Id] = deriving
    implicit def put: Put[Id] = deriving

    // circe instances
    implicit def encoder: Encoder[Id] = deriving
    implicit def decoder: Decoder[Id] = deriving
  }

  @newtype case class TxId(value: String)

  object TxId {
    // doobie instances
    implicit def get: Get[TxId] = deriving
    implicit def put: Put[TxId] = deriving

    // circe instances
    implicit def encoder: Encoder[TxId] = deriving
    implicit def decoder: Decoder[TxId] = deriving
  }

  @newtype case class BoxId(value: String)

  object BoxId {
    // doobie instances
    implicit def get: Get[BoxId] = deriving
    implicit def put: Put[BoxId] = deriving

    // circe instances
    implicit def encoder: Encoder[BoxId] = deriving
    implicit def decoder: Decoder[BoxId] = deriving
  }

  @newtype case class AssetId(value: String)

  object AssetId {
    // doobie instances
    implicit def get: Get[AssetId] = deriving
    implicit def put: Put[AssetId] = deriving

    // circe instances
    implicit def encoder: Encoder[AssetId] = deriving
    implicit def decoder: Decoder[AssetId] = deriving
  }

  type Address = String Refined Base58Spec

  type HexString = String Refined HexStringSpec

  type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]
}
