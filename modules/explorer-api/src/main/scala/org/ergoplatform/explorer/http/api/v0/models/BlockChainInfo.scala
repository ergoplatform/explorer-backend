package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import sttp.tapir.{Schema, Validator}

final case class BlockChainInfo(
  version: String,
  supply: Long,
  transactionAverage: Int, // avg. number of transactions per block.
  hashRate: Long
)

object BlockChainInfo {

  implicit val codec: Codec[BlockChainInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[BlockChainInfo] =
    Schema
      .derived[BlockChainInfo]
      .modify(_.version)(_.description("Network protocol version"))
      .modify(_.supply)(_.description("Total supply in nanoErgs"))
      .modify(_.transactionAverage)(_.description("Average number of transactions per block"))
      .modify(_.hashRate)(_.description("Network hash rate"))

  implicit val validator: Validator[BlockChainInfo] = schema.validator

  def empty: BlockChainInfo = BlockChainInfo("0.0.0", 0L, 0, 0L)
}
