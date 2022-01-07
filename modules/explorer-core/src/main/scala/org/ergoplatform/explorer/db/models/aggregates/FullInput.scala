package org.ergoplatform.explorer.db.models.aggregates

import io.circe.Json
import org.ergoplatform.explorer.db.models.Input
import org.ergoplatform.explorer.{Address, BlockId, HexString, TxId}

/** Input entity enriched with a data from corresponding output.
  */
final case class FullInput(
  input: Input,
  outputHeaderId: BlockId,
  outputTxId: TxId,
  value: Long, // amount of nanoERG in thee corresponding box
  outputIndex: Int, // index of the output in the transaction
  outputGlobalIndex: Long,
  outputCreatedAt: Int,
  outputSettledAt: Int,
  ergoTree: HexString, // serialized and hex-encoded ErgoTree
  address: Address, // an address derived from ergoTree
  additionalRegisters: Json
)
