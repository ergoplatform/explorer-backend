package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.aggregates.AggregatedAsset
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class TokenAmount(tokenId: TokenId, amount: Long, decimals: Int, name: Option[String])

object TokenAmount {

  def apply(a: AggregatedAsset): TokenAmount =
    TokenAmount(a.tokenId, a.totalAmount, a.decimals.getOrElse(0), a.name)

  implicit val schema: Schema[TokenAmount] =
    Schema
      .derived[TokenAmount]
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.amount)(_.description("Token amount"))
      .modify(_.decimals)(_.description("Number of decimals"))
      .modify(_.name)(_.description("Token name"))

  implicit val validator: Validator[TokenAmount] = schema.validator
}
