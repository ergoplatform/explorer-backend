package org.ergoplatform.explorer.db.models

import io.circe.Json
import org.ergoplatform.explorer._

final case class AnyOutput(
  boxId: BoxId,
  txId: TxId,
  headerId: Option[BlockId],
  value: Long,
  creationHeight: Int,
  settlementHeight: Option[Int],
  index: Int,
  globalIndex: Option[Long],
  ergoTree: HexString,
  ergoTreeTemplateHash: ErgoTreeTemplateHash,
  address: Address,
  additionalRegisters: Json,
  timestamp: Option[Long],
  mainChain: Option[Boolean],
  spendingTxId: Option[TxId]
)

