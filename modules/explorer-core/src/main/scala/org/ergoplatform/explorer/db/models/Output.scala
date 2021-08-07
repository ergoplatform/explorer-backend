package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer._

/** Represents `node_outputs` table.
  */
final case class Output(
  boxId: BoxId,
  txId: TxId,
  headerId: BlockId,
  value: Long, // amount of nanoERG in thee corresponding box
  creationHeight: Int, // the height this output was created
  settlementHeight: Int, // the height this output got fixed in blockchain
  index: Int, // index of the output in the transaction
  globalIndex: Long,
  ergoTree: HexString, // serialized and hex-encoded ErgoTree
  ergoTreeTemplateHash: ErgoTreeTemplateHash, // hash of serialized and hex-encoded ErgoTree template
  address: Address, // an address derived from ergoTree
  additionalRegisters: Json, // arbitrary key-value dictionary
  timestamp: Long, // time output appeared in the blockchain
  mainChain: Boolean // chain status, `true` if this output resides in main chain
)
