package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.TxId

/** Represents `node_u_transactions` table (Unconfirmed Transaction).
  */
final case class UTransaction(
  id: TxId,
  creationTimestamp: Long, // approx time transaction was created (appeared in mempool)
  size: Int                // transaction size in bytes
)
