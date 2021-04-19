package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedDataInput, ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.db.models.{AdProof, BlockExtension, Header, Transaction}
import sttp.tapir.{Schema, Validator}

final case class FullBlockInfo(
  header: HeaderInfo,
  blockTransactions: List[TransactionInfo],
  extension: BlockExtensionInfo,
  adProofs: Option[HexString]
)

object FullBlockInfo {

  implicit val codec: Codec[FullBlockInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[FullBlockInfo] =
    Schema
      .derived[FullBlockInfo]
      .modify(_.adProofs)(_.description("Serialized hex-encoded AD Proofs"))

  implicit val validator: Validator[FullBlockInfo] = schema.validator

  def apply(
    h: Header,
    txs: List[Transaction],
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    dataInputs: List[ExtendedDataInput],
    outputs: List[ExtendedOutput],
    assets: List[ExtendedAsset],
    extension: BlockExtension,
    adProof: Option[AdProof],
    blockSize: Int
  ): FullBlockInfo = {
    val txsInfo            = TransactionInfo.batch(txs.map(_ -> numConfirmations), inputs, dataInputs, outputs, assets)
    val headerInfo         = HeaderInfo(h, blockSize)
    val adProofInfo        = adProof.map(_.proofBytes)
    val blockExtensionInfo = BlockExtensionInfo(extension)
    new FullBlockInfo(headerInfo, txsInfo, blockExtensionInfo, adProofInfo)
  }
}
