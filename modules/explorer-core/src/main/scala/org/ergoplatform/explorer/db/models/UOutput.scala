package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer._

/** Represents `node_u_outputs` table (Unconfirmed Output, which is a part of Unconfirmed Transaction).
  */
final case class UOutput(
  boxId: BoxId,
  txId: TxId,
  value: Long, // amount of nanoERG in thee corresponding box
  creationHeight: Int, // the height this output was created
  index: Int, // index of the output in the transaction
  ergoTree: HexString, // serialized and hex-encoded ErgoTree
  ergoTreeTemplateHash: ErgoTreeTemplateHash, // hash of serialized and hex-encoded ErgoTree template
  address: Address, // an address derived from ergoTree (if possible)
  additionalRegisters: Json // arbitrary key-value dictionary
)
