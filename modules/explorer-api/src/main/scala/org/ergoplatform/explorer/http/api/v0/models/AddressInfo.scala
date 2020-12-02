package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.v0.models.AddressInfo.{Summary, Transactions}
import sttp.tapir.{Schema, Validator}
import sttp.tapir.generic.Derived

final case class AddressInfo(
  summary: Summary,
  transactions: Transactions
)

object AddressInfo {

  // Mirroring APIv0 structure to avoid handwriting codecs and schemas
  final case class Summary(id: Address)

  final case class Transactions(
    confirmed: Int,
    totalReceived: BigDecimal,
    confirmedBalance: Long,
    totalBalance: Long,
    confirmedTokensBalance: List[AssetSummary],
    totalTokensBalance: List[AssetSummary]
  )

  def apply(
    address: Address,
    confirmed: Int,
    totalReceived: BigDecimal,
    confirmedBalance: Long,
    totalBalance: Long,
    confirmedTokensBalance: List[AssetSummary],
    totalTokensBalance: List[AssetSummary]
  ): AddressInfo =
    new AddressInfo(
      Summary(address),
      Transactions(
        confirmed,
        totalReceived,
        confirmedBalance,
        totalBalance,
        confirmedTokensBalance,
        totalTokensBalance
      )
    )

  implicit private def summaryCodec: Codec[Summary]  = deriveCodec
  implicit private val txsCodec: Codec[Transactions] = deriveCodec
  implicit val codec: Codec[AddressInfo]             = deriveCodec

  implicit private def summarySchema: Schema[Summary] =
    Schema
      .derive[Summary]
      .modify(_.id)(_.description("Address identifier"))

  implicit private val summaryValidator: Validator[Summary] = Validator.derive

  implicit private def txsSchema: Schema[Transactions] =
    Schema
      .derive[Transactions]
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

  implicit private val txsValidator: Validator[Transactions] = Validator.derive

  implicit val schema: Schema[AddressInfo] = Schema.derive

  implicit val validator: Validator[AddressInfo] = Validator.derive
}
