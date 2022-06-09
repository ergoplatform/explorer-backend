package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.models.GenuineToken
import sttp.tapir.{Schema, Validator}

/** [[https://github.com/ergoplatform/eips/blob/master/eip-0021.md#genuine-tokens EIP0021: Genuine Tokens Verification]]: <br/>
  *
  * This EIP lists the common tokens of value with their unique identifier, so users as well as wallet and explorer applications can verify if a token is genuine.
  * The EIP is meant to be updated regularly when new tokens of value are added.
  */

@derive(encoder, decoder)
final case class GenuineTokenInfo(
  tokenId: TokenId,
  tokenName: String,
  uniqueName: Boolean,
  issuer: Option[String]
)

object GenuineTokenInfo {

  def apply(genuineToken: GenuineToken): GenuineTokenInfo =
    GenuineTokenInfo(
      genuineToken.id,
      genuineToken.tokenName,
      genuineToken.uniqueName,
      genuineToken.issuer
    )

  implicit val schema: Schema[GenuineTokenInfo] =
    Schema
      .derived[GenuineTokenInfo]
      .modify(_.tokenName)(_.description("Token Verbose Name"))
      .modify(_.tokenId)(_.description("Token ID"))
      .modify(_.uniqueName)(_.description("Boolean Token Verbose Name is unique"))
      .modify(_.issuer)(_.description("Token Issuer"))

  implicit val validator: Validator[GenuineTokenInfo] = schema.validator
}
