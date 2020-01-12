package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer.{BoxId, HexString, TxId}

/** Entity representing `node_u_inputs` table.
 */
final case class UInput(
  boxId: BoxId,
  txId: TxId,
  proofBytes: Option[HexString], // serialized and hex-encoded cryptographic proof
  extension: Json                // arbitrary key-value dictionary
)
