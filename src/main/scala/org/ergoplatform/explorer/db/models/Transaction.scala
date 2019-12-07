package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{Id, TxId}

/** Entity representing `node_transactions` table.
  */
final case class Transaction(
  id: TxId,
  headerId: Id,
  isCoinbase: Boolean,
  timestamp: Long,     // approx time output appeared in the blockchain
  size: Int            // transaction size in bytes
)
