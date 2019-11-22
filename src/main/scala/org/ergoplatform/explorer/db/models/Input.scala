package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer.{BoxId, HexString, TxId}

/** Entity representing `node_inputs` table.
  */
final case class Input(
  boxId: BoxId,
  txId: TxId,
  proofBytes: HexString,
  extension: Json,
  mainChain: Boolean
)
