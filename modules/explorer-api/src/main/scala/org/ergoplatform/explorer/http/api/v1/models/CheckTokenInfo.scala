package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.http.api.v1.TokenStatus
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class CheckTokenInfo(
  genuine: TokenStatus,
  token: Option[GenuineTokenInfo]
)

object CheckTokenInfo {

  implicit val schema: Schema[CheckTokenInfo] =
    Schema
      .derived[CheckTokenInfo]
      .modify(_.genuine)(
        _.description("Flag with 0 unknown, \n 1 verified, \n 2 suspicious, \n 3 blocked (see EIP-21)")
      )
      .modify(_.token)(_.description("Genuine Token Info"))
}
