package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.db.models.{AdProof, BlockExtension, Header, Transaction}
import org.ergoplatform.explorer.db.models.aggregates.{
  ExtendedAsset,
  ExtendedDataInput,
  ExtendedInput,
  ExtendedOutput,
  FullInput
}
import sttp.tapir.{Schema, Validator}

final case class FullBlockInfo(
  header: HeaderInfo,
  blockTransactions: List[TransactionInfo],
  extension: BlockExtensionInfo,
  adProofs: Option[HexString]
)

object FullBlockInfo {

  implicit val codec: Codec[FullBlockInfo] = deriveCodec

  implicit val schema: Schema[FullBlockInfo] =
    Schema
      .derived[FullBlockInfo]
      .modify(_.adProofs)(_.description("Serialized hex-encoded AD Proofs"))

  implicit val validator: Validator[FullBlockInfo] = schema.validator

  def apply(
    h: Header,
    txs: List[Transaction],
    numConfirmations: Int,
    inputs: List[FullInput],
    dataInputs: List[ExtendedDataInput],
    outputs: List[ExtendedOutput],
    inAssets: List[ExtendedAsset],
    outAssets: List[ExtendedAsset],
    extension: BlockExtension,
    adProof: Option[AdProof],
    blockSize: Int
  ): FullBlockInfo = {
    val txsInfo = TransactionInfo.unFlattenBatch(
      txs.map(_ -> numConfirmations),
      inputs,
      outputs,
      inAssets,
      outAssets
    )
    val headerInfo         = HeaderInfo(h, blockSize)
    val adProofInfo        = adProof.map(_.proofBytes)
    val blockExtensionInfo = BlockExtensionInfo(extension)
    new FullBlockInfo(headerInfo, txsInfo, blockExtensionInfo, adProofInfo)
  }
}
