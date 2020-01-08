package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.http.api.v0.models.AddressInfo.{Summary, Transactions}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class AddressInfo(
  summary: Summary,
  transactions: Transactions
)

object AddressInfo {

  // Mirroring APIv0 structure to avoid handwriting codecs and schemas
  final case class Summary(id: AddressInfo)

  final case class Transactions(
    confirmed: Int,
    totalReceived: BigDecimal,
    confirmedBalance: Long,
    totalBalance: Long,
    confirmedTokensBalance: List[AssetInfo],
    totalTokensBalance: List[AssetInfo]
  )

  implicit private lazy val summaryCodec: Codec[Summary] = deriveCodec
  implicit private val txsCodec: Codec[Transactions]     = deriveCodec
  implicit val codec: Codec[AddressInfo]                 = deriveCodec

  implicit private lazy val summarySchema: Schema[Summary] =
    implicitly[Derived[Schema[Summary]]].value
      .modify(_.id)(_.description("Address identifier"))
  implicit private lazy val txsSchema: Schema[Transactions] =
    implicitly[Derived[Schema[Transactions]]].value
      .modify(_.confirmed)(_.description("Number of confirmed txs"))
      .modify(_.totalReceived)(_.description("Total number of received nanoErgs"))
      .modify(_.confirmedBalance)(
        _.description("Confirmed balance of address in nanoErgs")
      )
      .modify(_.totalBalance)(
        _.description("Total (confirmed + unconfirmed) balance of address in nanoErgs")
      )
      .modify(_.confirmedTokensBalance)(
        _.description("Confirmed tokens balance of address")
      )
      .modify(_.totalTokensBalance)(
        _.description("Total (confirmed + unconfirmed) tokens balance of address")
      )
  implicit val schema: Schema[AddressInfo] =
    implicitly[Derived[Schema[AddressInfo]]].value
}
