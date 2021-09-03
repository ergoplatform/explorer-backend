package org.ergoplatform.explorer.db.models.aggregates

import io.circe.Json
import org.ergoplatform.explorer.db.models.UDataInput
import org.ergoplatform.explorer.{Address, BlockId, ErgoTree, TxId}

/** Unconfirmed input entity enriched with a data from corresponding output.
 */
final case class ExtendedUDataInput(
  input: UDataInput,
  value: Long,
  outputTxId: TxId,
  outputBlockId: Option[BlockId],
  outputIndex: Int,
  ergoTree: ErgoTree,
  address: Address,
  additionalRegisters: Json
)
