package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.Token
import org.ergoplatform.explorer.{BoxId, TokenId, TokenType}
import sttp.tapir.{Schema, Validator}

final case class TokenInfo(
  id: TokenId,
  boxId: BoxId,
  name: String,
  description: String,
  `type`: TokenType,
  decimals: Int,
  emissionAmount: Long
)

object TokenInfo {

  def apply(token: Token): TokenInfo =
    TokenInfo(
      token.id,
      token.boxId,
      token.name,
      token.description,
      token.`type`,
      token.decimals,
      token.emissionAmount
    )

  implicit val codec: Codec[TokenInfo] = deriveCodec

  implicit val schema: Schema[TokenInfo] =
    Schema
      .derive[TokenInfo]
      .modify(_.id)(_.description("ID of the asset"))
      .modify(_.boxId)(_.description("Box ID this asset was issued by"))
      .modify(_.name)(_.description("Name of the asset"))
      .modify(_.description)(_.description("Description of the asset"))
      .modify(_.`type`)(_.description("Asset type (token standard)"))
      .modify(_.decimals)(_.description("Number of decimal places"))
      .modify(_.emissionAmount)(_.description("Number of decimal places"))

  implicit val validator: Validator[TokenInfo] = Validator.derive
}
