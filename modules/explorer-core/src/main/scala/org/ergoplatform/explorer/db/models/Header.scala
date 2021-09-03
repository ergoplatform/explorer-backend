package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{BlockId, HexString}

/** Represents `node_headers` table.
  */
final case class Header(
  id: BlockId,
  parentId: BlockId,
  version: Byte,
  height: Int,
  nBits: Long,
  difficulty: BigDecimal,
  timestamp: Long,
  stateRoot: HexString,
  adProofsRoot: HexString,
  transactionsRoot: HexString,
  extensionHash: HexString,
  minerPk: HexString,
  w: HexString, // PoW one time PK
  n: HexString, // PoW nonce
  d: String, // PoW distance
  votes: String, // hex-encoded votes for a soft-fork and parameters
  mainChain: Boolean // chain status, `true` if this header resides in main chain.
)
