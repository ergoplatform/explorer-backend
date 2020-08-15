package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedDataInput, ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.db.models.{AdProof, Asset, BlockExtension, Header, Transaction}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class FullBlockInfo(
  header: HeaderInfo,
  blockTransactions: List[TransactionInfo],
  extension: BlockExtensionInfo,
  adProofs: Option[HexString]
)

object FullBlockInfo {

  implicit val codec: Codec[FullBlockInfo] = deriveCodec

  implicit val schema: Schema[FullBlockInfo] =
    implicitly[Derived[Schema[FullBlockInfo]]].value
      .modify(_.adProofs)(_.description("Serialized hex-encoded AD Proofs"))

  def apply(
    h: Header,
    txs: List[Transaction],
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    dataInputs: List[ExtendedDataInput],
    outputs: List[ExtendedOutput],
    assets: List[Asset],
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
