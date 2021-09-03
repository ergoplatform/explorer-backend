package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BlockId, BoxId, TxId}

/** Represents `node_data_inputs` table.
  */
final case class DataInput(
  boxId: BoxId,
  txId: TxId,
  headerId: BlockId,
  index: Int, // index  of the input in the transaction
  mainChain: Boolean // chain status, `true` if this input resides in main chain.
)
