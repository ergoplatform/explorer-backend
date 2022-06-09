package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedDataInput, ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.db.models.{Header, Transaction}
import org.ergoplatform.explorer.http.api.v0.models.{HeaderInfo, TransactionInfo => TxInf}
import sttp.tapir.{Schema, Validator}

final case class BlockSummaryV1(
  header: HeaderInfo,
  blockTransactions: List[TxInf]
)

object BlockSummaryV1 {

  implicit val codec: Codec[BlockSummaryV1] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[BlockSummaryV1] =
    Schema
      .derived[BlockSummaryV1]

  implicit val validator: Validator[BlockSummaryV1] = schema.validator

  def apply(
    h: Header,
    txs: List[Transaction],
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    dataInputs: List[ExtendedDataInput],
    outputs: List[ExtendedOutput],
    assets: List[ExtendedAsset],
    blockSize: Int
  ): BlockSummaryV1 = {
    val txsInfo    = TxInf.batch(txs.map(_ -> numConfirmations), inputs, dataInputs, outputs, assets)
    val headerInfo = HeaderInfo(h, blockSize)
    new BlockSummaryV1(headerInfo, txsInfo)
  }
}
