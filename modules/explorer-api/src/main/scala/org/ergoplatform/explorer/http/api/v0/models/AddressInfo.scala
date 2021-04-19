package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.v0.models.AddressInfo.{Summary, Transactions}
import sttp.tapir.{Schema, Validator}

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

  implicit private def summaryCodec: Codec[Summary]  = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)
  implicit private val txsCodec: Codec[Transactions] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)
  implicit val codec: Codec[AddressInfo]             = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit private def summarySchema: Schema[Summary] =
    Schema
      .derived[Summary]
      .modify(_.id)(_.description("Address identifier"))

  implicit private val summaryValidator: Validator[Summary] = Schema
    .derived[Summary]
    .validator

  implicit private def txsSchema: Schema[Transactions] =
    Schema
      .derived[Transactions]
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

  implicit private val txsValidator: Validator[Transactions] = Schema
    .derived[Transactions]
    .validator

  implicit val schema: Schema[AddressInfo] = Schema.derived[AddressInfo]

  implicit val validator: Validator[AddressInfo] = schema.validator
}
