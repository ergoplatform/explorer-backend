package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer.{BoxId, HexString, TxId}

/** Represents `node_u_inputs` table (Unconfirmed Input, which is a part of Unconfirmed Transaction).
 */
final case class UInput(
  boxId: BoxId,
  txId: TxId,
  index: Int, // index of the input in the transaction
  proofBytes: Option[HexString], // serialized and hex-encoded cryptographic proof
  extension: Json                // arbitrary key-value dictionary
)
