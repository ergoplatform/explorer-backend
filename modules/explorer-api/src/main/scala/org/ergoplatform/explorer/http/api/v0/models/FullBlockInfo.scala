package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.db.models.{AdProof, Asset, BlockExtension, Header, Transaction}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class FullBlockInfo(
  headerInfo: HeaderInfo,
  transactionsInfo: List[TransactionInfo],
  extension: BlockExtensionInfo,
  adProof: Option[AdProofInfo]
)

object FullBlockInfo {

  implicit val codec: Codec[FullBlockInfo] = deriveCodec

  implicit val schema: Schema[FullBlockInfo] =
    implicitly[Derived[Schema[FullBlockInfo]]].value

  def apply(
    h: Header,
    txs: List[Transaction],
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    outputs: List[ExtendedOutput],
    assets: List[Asset],
    extension: BlockExtension,
    adProof: Option[AdProof],
    blockSize: Int
  ): FullBlockInfo = {
    val txsInfo     = TransactionInfo.batch(numConfirmations, txs, inputs, outputs, assets)
    val headerInfo  = HeaderInfo(h, blockSize)
    val adProofInfo = adProof.map { AdProofInfo.apply }
    val blockExtensionInfo = BlockExtensionInfo(extension)
    new FullBlockInfo(headerInfo, txsInfo, blockExtensionInfo, adProofInfo)
  }
}
