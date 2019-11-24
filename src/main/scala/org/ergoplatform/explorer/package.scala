package org.ergoplatform

import cats.ApplicativeError
import cats.syntax.either._
import cats.syntax.functor._
import doobie.util.{Get, Put}
import doobie.refined.implicits._
import eu.timepit.refined.refineV
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{HexStringSpec, MatchesRegex}
import io.circe.{Decoder, Encoder}
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import org.ergoplatform.explorer.constraints._

package object explorer {

  object constraints {

    type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

    type AddressType = String Refined Base58Spec

    type HexStringType = String Refined HexStringSpec
  }

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

  @newtype case class Address(value: AddressType) {
    final def unwrapped: String = value.value
  }

  object Address {
    // doobie instances
    implicit def get: Get[Address] = deriving
    implicit def put: Put[Address] = deriving

    // circe instances
    implicit def encoder: Encoder[Address] = deriving
    implicit def decoder: Decoder[Address] = deriving

    def fromString[F[_]](
      s: String
    )(implicit F: ApplicativeError[F, Throwable]): F[Address] =
      refineV[Base58Spec](s)
        .leftMap[Throwable](e => new Exception(s"Refinement failed: $e"))
        .liftTo[F]
        .map(Address.apply)
  }

  @newtype case class HexString(value: HexStringType) {
    final def unwrapped: String = value.value
  }

  object HexString {
    // doobie instances
    implicit def get: Get[HexString] = deriving
    implicit def put: Put[HexString] = deriving

    // circe instances
    implicit def encoder: Encoder[HexString] = deriving
    implicit def decoder: Decoder[HexString] = deriving

    def fromString[F[_]](
      s: String
    )(implicit F: ApplicativeError[F, Throwable]): F[HexString] =
      refineV[HexStringSpec](s)
        .leftMap[Throwable](e => new Exception(s"Refinement failed: $e"))
        .liftTo[F]
        .map(HexString.apply)
  }
}
