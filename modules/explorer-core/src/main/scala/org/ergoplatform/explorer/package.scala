package org.ergoplatform

import cats.Applicative
import cats.instances.either._
import cats.syntax.either._
import cats.syntax.functor._
import doobie.util.{Get, Put}
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.boolean.OneOf
import eu.timepit.refined.predicates.all.Equal
import eu.timepit.refined.string.{HexStringSpec, MatchesRegex, Url}
import eu.timepit.refined.{refineMV, refineV, W}
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.constraints._
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import scorex.util.encode.Base16
import shapeless.HNil
import sttp.tapir.json.circe._
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema, Validator}
import tofu.Raise.ContravariantRaise
import tofu.logging.Loggable
import tofu.syntax.raise._

import scala.util.Try

package object explorer {

  type CRaise[F[_], -E] = ContravariantRaise[F, E]

  object constraints {

    type OrderingSpec = MatchesRegex[W.`"^(?i)(asc|desc)$"`.T]

    type OrderingString = String Refined OrderingSpec

    type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

    type AddressType = String Refined Base58Spec

    type HexStringType = String Refined HexStringSpec

    type UrlStringType = String Refined Url
  }

  /** Persistent modifier id (header, block_transaction, etc.)
    */
  @newtype case class BlockId(value: HexString)

  object BlockId {
    // doobie instances
    implicit def get: Get[BlockId] = deriving
    implicit def put: Put[BlockId] = deriving

    // circe instances
    implicit def encoder: Encoder[BlockId] = deriving
    implicit def decoder: Decoder[BlockId] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[BlockId] = deriving

    implicit def jsonCodec: Codec.JsonCodec[BlockId] =
      HexString.jsonCodec.map(BlockId(_))(_.value)

    implicit def schema: Schema[BlockId] =
      Schema.schemaForString.description("Modifier ID").asInstanceOf[Schema[BlockId]]

    implicit def validator: Validator[BlockId] =
      implicitly[Validator[HexString]].contramap[BlockId](_.value)

    implicit def loggable: Loggable[BlockId] = deriving

    def fromString[
      F[_]: CRaise[*[_], RefinementFailed]: Applicative
    ](s: String): F[BlockId] =
      HexString.fromString(s).map(BlockId.apply)

    def fromStringUnsafe(s: String): BlockId = fromString[Try](s).get
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
      Schema.schemaForString.validator.contramap[TxId](_.value)
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
      Schema.schemaForString.validator.contramap[BoxId](_.value)

    def fromErgo(boxId: org.ergoplatform.ErgoBox.BoxId): BoxId =
      BoxId(Base16.encode(boxId))
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

    def fromStringUnsafe(s: String): TokenId = unsafeWrap(HexString.fromStringUnsafe(s))
  }

  @newtype case class TokenSymbol(value: String)

  object TokenSymbol {
    // doobie instances
    implicit def get: Get[TokenSymbol] = deriving
    implicit def put: Put[TokenSymbol] = deriving

    // circe instances
    implicit def encoder: Encoder[TokenSymbol] = deriving
    implicit def decoder: Decoder[TokenSymbol] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[TokenSymbol] = deriving

    implicit def jsonCodec: Codec.JsonCodec[TokenSymbol] = deriving

    implicit def schema: Schema[TokenSymbol] =
      Schema.schemaForString.description("Token Symbol").asInstanceOf[Schema[TokenSymbol]]

    implicit def validator: Validator[TokenSymbol] = Validator.minLength(2).contramap(_.value)

    def fromStringUnsafe(s: String): TokenSymbol = TokenSymbol(s)
  }

  @newtype case class ErgoTreeTemplateHash(value: HexString)

  object ErgoTreeTemplateHash {
    // doobie instances
    implicit def get: Get[ErgoTreeTemplateHash] = deriving
    implicit def put: Put[ErgoTreeTemplateHash] = deriving

    // circe instances
    implicit def encoder: Encoder[ErgoTreeTemplateHash] = deriving
    implicit def decoder: Decoder[ErgoTreeTemplateHash] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[ErgoTreeTemplateHash] = deriving

    implicit def jsonCodec: Codec.JsonCodec[ErgoTreeTemplateHash] =
      HexString.jsonCodec.map(ErgoTreeTemplateHash(_))(_.value)

    implicit def schema: Schema[ErgoTreeTemplateHash] =
      Schema.schemaForString.description("ErgoTree Template").asInstanceOf[Schema[ErgoTreeTemplateHash]]

    implicit def validator: Validator[ErgoTreeTemplateHash] =
      implicitly[Validator[HexString]].contramap[ErgoTreeTemplateHash](_.value)

    def fromStringUnsafe(s: String): ErgoTreeTemplateHash = unsafeWrap(HexString.fromStringUnsafe(s))
  }

  @newtype case class ErgoTree(value: HexString)

  object ErgoTree {
    // doobie instances
    implicit def get: Get[ErgoTree] = deriving
    implicit def put: Put[ErgoTree] = deriving

    implicit val encoder: io.circe.Encoder[ErgoTree] = deriving
    implicit val decoder: io.circe.Decoder[ErgoTree] = deriving

    // tapir instances
    implicit def plainCodec: Codec.PlainCodec[ErgoTree] = deriving

    implicit def jsonCodec: Codec.JsonCodec[ErgoTree] =
      HexString.jsonCodec.map(ErgoTree(_))(_.value)

    implicit def schema: Schema[ErgoTree] =
      Schema.schemaForString.description("ErgoTree Template").asInstanceOf[Schema[ErgoTree]]

    implicit def validator: Validator[ErgoTree] =
      implicitly[Validator[HexString]].contramap[ErgoTree](_.value)
  }

  @newtype case class TokenType(value: String)

  object TokenType {

    val Eip004: TokenType = "EIP-004".coerce[TokenType]

    implicit def schema: Schema[TokenType] =
      Schema.schemaForString.description("Token type").asInstanceOf[Schema[TokenType]]

    implicit def validator: Validator[TokenType] =
      Schema.schemaForString.validator.contramap[TokenType](_.value)

    // doobie instances
    implicit def get: Get[TokenType] = deriving
    implicit def put: Put[TokenType] = deriving

    // circe instances
    implicit def encoder: Encoder[TokenType] = deriving
    implicit def decoder: Decoder[TokenType] = deriving
  }

  // Ergo Address
  @newtype case class Address(value: AddressType) {
    final def unwrapped: String = value.value
  }

  object Address {
    // doobie instances
    implicit def get: Get[Address] = Get[String].map(fromStringUnsafe)
    implicit def put: Put[Address] = Put[String].contramap(_.unwrapped)

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
      Schema.schemaForString.validator.contramap[Address](_.unwrapped)

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

    def fromStringUnsafe(s: String): Address = unsafeWrap(refineV[Base58Spec].unsafeFrom(s))
  }

  @newtype case class HexString(value: HexStringType) {
    final def unwrapped: String  = value.value
    final def bytes: Array[Byte] = Base16.decode(unwrapped).get
  }

  object HexString {
    // doobie instances
    implicit def get: Get[HexString] = Get[String].map(fromStringUnsafe)
    implicit def put: Put[HexString] = Put[String].contramap(_.unwrapped)

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
      Schema.schemaForString.validator.contramap[HexString](_.unwrapped)

    implicit def loggable: Loggable[HexString] = Loggable.stringValue.contramap(_.unwrapped)

    def fromString[
      F[_]: CRaise[*[_], RefinementFailed]: Applicative
    ](s: String): F[HexString] =
      refineV[HexStringSpec](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(HexString.apply)

    def fromStringUnsafe(s: String): HexString = unsafeWrap(refineV[HexStringSpec].unsafeFrom(s))
  }

  @newtype case class UrlString(value: UrlStringType) {
    final def unwrapped: String = value.value
  }

  object UrlString {

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

  private def deriveCodec[A, CF <: CodecFormat, T](
    at: A => Either[Throwable, T],
    ta: T => A
  )(implicit c: Codec[String, A, CF]): Codec[String, T, CF] =
    c.mapDecode { x =>
      at(x).fold(DecodeResult.Error(x.toString, _), DecodeResult.Value(_))
    }(ta)
}
