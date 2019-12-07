package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}

/** Entity representing `node_outputs` table.
  */
final case class Output(
  boxId: BoxId,
  txId: TxId,
  value: Long,
  creationHeight: Int,
  index: Int,
  ergoTree: HexString,
  addressOpt: Option[Address],
  additionalRegisters: Json,
  timestamp: Long,             // approx time output appeared in the blockchain
  mainChain: Boolean           // chain status, `true` if this output resides in main chain
)
