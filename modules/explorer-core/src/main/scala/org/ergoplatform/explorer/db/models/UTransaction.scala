package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.TxId

/** Entity representing `node_u_transactions` table.
  */
final case class UTransaction(
  id: TxId,
  creationTimestamp: Long, // approx time transaction was created (appeared in mempool)
  size: Int                // transaction size in bytes
)
