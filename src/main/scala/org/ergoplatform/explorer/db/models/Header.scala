package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{HexString, Id}

/** Entity representing `node_headers` table.
  */
final case class Header(
  id: Id,
  parentId: Id,
  version: Short,
  height: Int,
  nBits: Long,
  difficulty: Long,
  timestamp: Long,
  stateRoot: HexString,
  adProofsRoot: HexString,
  transactionsRoot: HexString,
  extensionHash: HexString,
  minerPk: HexString,
  w: HexString,
  n: HexString,
  d: String,
  votes: String,
  mainChain: Boolean           // chain status, `true` if this header resides in main chain.
)
