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
  address: Address,
  additionalRegisters: Json,
  timestamp: Long,
  mainChain: Boolean         // chain status, `true` if this output resides in main chain.
)
