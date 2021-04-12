package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.db.models.Token
import org.ergoplatform.explorer.{BoxId, TokenId, TokenType}
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class TokenInfo(
  id: TokenId,
  boxId: BoxId,
  emissionAmount: Long,
  name: Option[String],
  description: Option[String],
  `type`: Option[TokenType],
  decimals: Option[Int]
)

object TokenInfo {

  def apply(token: Token): TokenInfo =
    TokenInfo(
      token.id,
      token.boxId,
      token.emissionAmount,
      token.name,
      token.description,
      token.`type`,
      token.decimals
    )

  implicit val schema: Schema[TokenInfo] =
    Schema
      .derived[TokenInfo]
      .modify(_.id)(_.description("ID of the asset"))
      .modify(_.boxId)(_.description("Box ID this asset was issued by"))
      .modify(_.emissionAmount)(_.description("Number of decimal places"))
      .modify(_.name)(_.description("Name of the asset"))
      .modify(_.description)(_.description("Description of the asset"))
      .modify(_.`type`)(_.description("Asset type (token standard)"))
      .modify(_.decimals)(_.description("Number of decimal places"))

  implicit val validator: Validator[TokenInfo] = schema.validator
}
