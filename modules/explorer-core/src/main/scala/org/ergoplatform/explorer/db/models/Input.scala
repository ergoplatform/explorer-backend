package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer.{BoxId, HexString, TxId}

/** Entity representing `node_inputs` table.
  */
final case class Input(
  boxId: BoxId,
  txId: TxId,
  proofBytes: Option[HexString], // serialized and hex-encoded cryptographic proof
  extension: Json, // arbitrary key-value dictionary
  mainChain: Boolean // chain status, `true` if this input resides in main chain.
)
