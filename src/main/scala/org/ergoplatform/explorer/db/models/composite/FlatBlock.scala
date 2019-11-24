package org.ergoplatform.explorer.db.models.composite

import org.ergoplatform.explorer.db.models.{
  AdProof,
  Asset,
  BlockExtension,
  BlockInfo,
  Header,
  Input,
  Output,
  Transaction
}
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import org.ergoplatform.explorer.settings.ProtocolSettings

final case class FlatBlock(
  header: Header,
  info: BlockInfo,
  adProof: AdProof,
  extension: BlockExtension,
  txs: List[Transaction],
  inputs: List[Input],
  outputs: List[Output],
  assets: List[Asset]
)

object FlatBlock {

  def fromApi(
    apiBlock: ApiFullBlock,
    parentInfoOpt: Option[BlockInfo]
  )(protocolSettings: ProtocolSettings): FlatBlock = ???
}
