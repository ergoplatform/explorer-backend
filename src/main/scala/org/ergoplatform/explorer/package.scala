package org.ergoplatform

import cats.ApplicativeError
import cats.syntax.either._
import cats.syntax.functor._
import doobie.refined.implicits._
import doobie.util.{Get, Put}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.{refineV, W}
import eu.timepit.refined.string.{HexStringSpec, MatchesRegex, Url}
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.explorer.constraints._
import sttp.tapir.Codec

package object explorer {

  object constraints {

    type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

    type AddressType = String Refined Base58Spec

    type HexStringType = String Refined HexStringSpec

    type UrlStringType = String Refined Url
  }

  @newtype case class Id(value: String)

  object Id {
    // doobie instances
    implicit def get: Get[Id] = deriving
    implicit def put: Put[Id] = deriving

    // circe instances
    implicit def encoder: Encoder[Id] = deriving
    implicit def decoder: Decoder[Id] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[Id] = deriving
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
        .leftMap[Throwable](Err.RefinementFailed)
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
        .leftMap[Throwable](Err.RefinementFailed)
        .liftTo[F]
        .map(HexString.apply)
  }

  @newtype case class UrlString(value: UrlStringType) {
    final def unwrapped: String = value.value
  }

  object UrlString {
    // doobie instances
    implicit def get: Get[UrlString] = deriving
    implicit def put: Put[UrlString] = deriving

    // circe instances
    implicit def encoder: Encoder[UrlString] = deriving
    implicit def decoder: Decoder[UrlString] = deriving

    def fromString[F[_]](
      s: String
    )(implicit F: ApplicativeError[F, Throwable]): F[UrlString] =
      refineV[Url](s)
        .leftMap[Throwable](Err.RefinementFailed)
        .liftTo[F]
        .map(UrlString.apply)
  }
}
