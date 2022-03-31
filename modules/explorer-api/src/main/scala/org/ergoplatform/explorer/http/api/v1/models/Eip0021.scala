package org.ergoplatform.explorer.http.api.v1.models

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.protocol.models.GenuineTokens
import sttp.tapir.{Schema, Validator}
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.db.models.Eip0021TokenList

@derive(encoder, decoder)
final case class Eip0021(
  genuineTokens: List[GenuineTokens],
  blockedTokens: List[TokenId]
)

object Eip0021 {

  def apply(tl: Eip0021TokenList): Eip0021 =
    Eip0021(tl.genuineTokens, tl.blockedTokens)

  implicit val schema: Schema[Eip0021] =
    Schema
      .derived[Eip0021]
      .modify(_.blockedTokens)(_.description("Array[] Blocked token IDs"))
      .modify(_.genuineTokens)(_.description("Array[] Genuine Tokens"))

  implicit val validator: Validator[Eip0021] = schema.validator
}
