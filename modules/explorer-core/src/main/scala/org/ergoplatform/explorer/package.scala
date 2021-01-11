package org.ergoplatform

import cats.Applicative
import cats.instances.either._
import cats.instances.string._
import cats.syntax.either._
import cats.syntax.functor._
import doobie.refined.implicits._
import doobie.util.{Get, Put}
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.string.{HexStringSpec, MatchesRegex, Url}
import eu.timepit.refined.{W, refineV}
import io.circe.refined._
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.constraints._
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import scorex.util.encode.Base16
import sttp.tapir.json.circe._
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema, Validator}
import tofu.Raise.ContravariantRaise
import tofu.syntax.raise._

package object explorer {

  type CRaise[F[_], -E] = ContravariantRaise[F, E]

  object constraints {

    final case class ValidOrdering()

    object ValidOrdering {

      implicit def validLongValidate: Validate.Plain[String, ValidOrdering] =
        Validate.fromPredicate(
          x => x.toLowerCase == "asc" || x.toLowerCase == "desc",
          _ => "ValidOrdering",
          ValidOrdering()
        )
    }

    type OrderingString = String Refined ValidOrdering

    type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

    type AddressType = String Refined Base58Spec

    type HexStringType = String Refined HexStringSpec

    type UrlStringType = String Refined Url
  }

  /** Persistent modifier id (header, block_transaction, etc.)
    */
  @newtype case class Id(value: HexString)

  object Id {
    // doobie instances
    implicit def get: Get[Id] = deriving
    implicit def put: Put[Id] = deriving

    // circe instances
    implicit def encoder: Encoder[Id] = deriving
    implicit def decoder: Decoder[Id] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[Id] = deriving

    implicit def jsonCodec: Codec.JsonCodec[Id] =
      HexString.jsonCodec.map(Id(_))(_.value)

    implicit def schema: Schema[Id] =
      Schema.schemaForString.description("Modifier ID").asInstanceOf[Schema[Id]]

    implicit def validator: Validator[Id] =
      implicitly[Validator[HexString]].contramap[Id](_.value)

    def fromString[
      F[_]: CRaise[*[_], RefinementFailed]: Applicative
    ](s: String): F[Id] =
      HexString.fromString(s).map(Id.apply)
  }

  @newtype case class TxId(value: String)

  object TxId {
    // doobie instances
    implicit def get: Get[TxId] = deriving
    implicit def put: Put[TxId] = deriving

    // circe instances
    implicit def encoder: Encoder[TxId] = deriving
    implicit def decoder: Decoder[TxId] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[TxId] = deriving

    implicit def jsonCodec: Codec.JsonCodec[TxId] =
      implicitly[Codec.JsonCodec[String]].map(TxId(_))(_.value)

    implicit def schema: Schema[TxId] =
      Schema.schemaForString.description("Transaction ID").asInstanceOf[Schema[TxId]]

    implicit def validator: Validator[TxId] =
      Validator.validatorForString.contramap[TxId](_.value)
  }

  @newtype case class BoxId(value: String)

  object BoxId {
    // doobie instances
    implicit def get: Get[BoxId] = deriving
    implicit def put: Put[BoxId] = deriving

    // circe instances
    implicit def encoder: Encoder[BoxId] = deriving
    implicit def decoder: Decoder[BoxId] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[BoxId] = deriving

    implicit def jsonCodec: Codec.JsonCodec[BoxId] =
      implicitly[Codec.JsonCodec[String]].map(BoxId(_))(_.value)

    implicit def schema: Schema[BoxId] =
      Schema.schemaForString.description("Box ID").asInstanceOf[Schema[BoxId]]

    implicit def validator: Validator[BoxId] =
      Validator.validatorForString.contramap[BoxId](_.value)
  }

  @newtype case class TokenId(value: HexString)

  object TokenId {
    // doobie instances
    implicit def get: Get[TokenId] = deriving
    implicit def put: Put[TokenId] = deriving

    // circe instances
    implicit def encoder: Encoder[TokenId] = deriving
    implicit def decoder: Decoder[TokenId] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[TokenId] = deriving

    implicit def jsonCodec: Codec.JsonCodec[TokenId] =
      HexString.jsonCodec.map(TokenId(_))(_.value)

    implicit def schema: Schema[TokenId] =
      Schema.schemaForString.description("Token ID").asInstanceOf[Schema[TokenId]]

    implicit def validator: Validator[TokenId] =
      implicitly[Validator[HexString]].contramap[TokenId](_.value)

    def fromString[
      F[_]: CRaise[*[_], RefinementFailed]: Applicative
    ](s: String): F[TokenId] =
      HexString.fromString(s).map(TokenId.apply)
  }

  @newtype case class TokenType(value: String)

  object TokenType {

    val Eip004: TokenType = "EIP-004".coerce[TokenType]

    // doobie instances
    implicit def get: Get[TokenType] = deriving
    implicit def put: Put[TokenType] = deriving

    // circe instances
    implicit def encoder: Encoder[TokenType] = deriving
    implicit def decoder: Decoder[TokenType] = deriving
  }

  sealed abstract class RegisterId extends EnumEntry

  object RegisterId extends Enum[RegisterId] with CirceEnum[RegisterId] {

    case object R0 extends RegisterId
    case object R1 extends RegisterId
    case object R2 extends RegisterId
    case object R3 extends RegisterId
    case object R4 extends RegisterId
    case object R5 extends RegisterId
    case object R6 extends RegisterId
    case object R7 extends RegisterId
    case object R8 extends RegisterId
    case object R9 extends RegisterId

    val values = findValues

    implicit val keyDecoder: KeyDecoder[RegisterId] = withNameOption
    implicit val keyEncoder: KeyEncoder[RegisterId] = _.entryName

    implicit val get: Get[RegisterId] =
      Get[String].temap(s => withNameEither(s).leftMap(_ => s"No such RegisterId [$s]"))

    implicit val put: Put[RegisterId] =
      Put[String].contramap[RegisterId](_.entryName)
  }

  // Ergo Address
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

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[Address] =
      deriveCodec[String, CodecFormat.TextPlain, Address](
        fromString[Either[Throwable, *]](_),
        _.unwrapped
      )

    implicit def jsonCodec: Codec.JsonCodec[Address] =
      deriveCodec[String, CodecFormat.Json, Address](
        fromString[Either[Throwable, *]](_),
        _.unwrapped
      )

    implicit def schema: Schema[Address] =
      Schema.schemaForString.description("Ergo Address").asInstanceOf[Schema[Address]]

    implicit def validator: Validator[Address] =
      Validator.validatorForString.contramap[Address](_.unwrapped)

    implicit def configReader: ConfigReader[Address] =
      implicitly[ConfigReader[String]].emap { s =>
        fromString[Either[RefinementFailed, *]](s).leftMap(e => CannotConvert(s, s"Refined", e.msg))
      }

    def fromString[
      F[_]: CRaise[*[_], RefinementFailed]: Applicative
    ](s: String): F[Address] =
      refineV[Base58Spec](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(Address.apply)
  }

  @newtype case class HexString(value: HexStringType) {
    final def unwrapped: String  = value.value
    final def bytes: Array[Byte] = Base16.decode(unwrapped).get
  }

  object HexString {
    // doobie instances
    implicit def get: Get[HexString] = deriving
    implicit def put: Put[HexString] = deriving

    // circe instances
    implicit def encoder: Encoder[HexString] = deriving
    implicit def decoder: Decoder[HexString] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[HexString] =
      deriveCodec[String, CodecFormat.TextPlain, HexString](
        fromString[Either[Throwable, *]](_),
        _.unwrapped
      )

    implicit def jsonCodec: Codec.JsonCodec[HexString] =
      deriveCodec[String, CodecFormat.Json, HexString](
        fromString[Either[Throwable, *]](_),
        _.unwrapped
      )

    implicit def schema: Schema[HexString] =
      Schema.schemaForString.description("Hex-encoded string").asInstanceOf[Schema[HexString]]

    implicit def validator: Validator[HexString] =
      Validator.validatorForString.contramap[HexString](_.unwrapped)

    def fromString[
      F[_]: CRaise[*[_], RefinementFailed]: Applicative
    ](s: String): F[HexString] =
      refineV[HexStringSpec](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
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

    implicit def configReader: ConfigReader[UrlString] =
      implicitly[ConfigReader[String]].emap { s =>
        fromString[Either[RefinementFailed, *]](s).leftMap(e => CannotConvert(s, s"Refined", e.msg))
      }

    def fromString[
      F[_]: CRaise[*[_], RefinementFailed]: Applicative
    ](s: String): F[UrlString] =
      refineV[Url](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(UrlString.apply)
  }

  @newtype case class ContractAttributes(value: Map[String, String])

  object ContractAttributes {
    // circe instances
    implicit def encoder: Encoder[ContractAttributes] = deriving
    implicit def decoder: Decoder[ContractAttributes] = deriving

    implicit def jsonCodec: Codec.JsonCodec[ContractAttributes] =
      implicitly[Codec.JsonCodec[Map[String, String]]]
        .map(ContractAttributes(_))(_.value)

    implicit def schema: Schema[ContractAttributes] =
      implicitly[Schema[Map[String, String]]].asInstanceOf[Schema[ContractAttributes]]

    implicit def validator: Validator[ContractAttributes] =
      implicitly[Validator[Map[String, String]]].contramap[ContractAttributes](_.value)
  }

  private def deriveCodec[A, CF <: CodecFormat, T](
    at: A => Either[Throwable, T],
    ta: T => A
  )(implicit c: Codec[String, A, CF]): Codec[String, T, CF] =
    c.mapDecode { x =>
      at(x).fold(DecodeResult.Error(x.toString, _), DecodeResult.Value(_))
    }(ta)
}
