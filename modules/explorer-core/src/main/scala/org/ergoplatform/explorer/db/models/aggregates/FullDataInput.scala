package org.ergoplatform.explorer.db.models.aggregates

import io.circe.Json
import org.ergoplatform.explorer.db.models.DataInput
import org.ergoplatform.explorer.{Address, BlockId, HexString, TxId}

final case class FullDataInput(
  input: DataInput,
  outputHeaderId: BlockId,
  outputTxId: TxId,
  value: Long, // amount of nanoERG in thee corresponding box
  outputIndex: Int, // index of the output in the transaction
  ergoTree: HexString, // serialized and hex-encoded ErgoTree
  address: Address, // an address derived from ergoTree
  additionalRegisters: Json
)
