package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{Id, TxId}

/** Represents `node_transactions` table.
  */
final case class Transaction(
  id: TxId,
  headerId: Id,
  inclusionHeight: Int,
  isCoinbase: Boolean,
  timestamp: Long, // approx time output appeared in the blockchain
  size: Int, // transaction size in bytes
  index: Int, // index of a transaction inside a block
  globalIndex: Long,
  mainChain: Boolean
) {

  def numConfirmations(bestHeight: Int): Int = bestHeight - inclusionHeight + 1
}
