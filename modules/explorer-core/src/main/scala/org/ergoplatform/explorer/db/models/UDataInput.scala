package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BoxId, TxId}

/** Represents `node_u_data_inputs` table (Unconfirmed Data Input, which is a part of Unconfirmed Transaction).
  */
final case class UDataInput(
  boxId: BoxId,
  txId: TxId,
  index: Int // index of the input in the transaction
)
