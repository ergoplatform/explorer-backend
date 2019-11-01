package org.ergoplatform.explorer.persistence.models

import org.ergoplatform.explorer.{HexString, Id}

final case class Header(
  id: Id,
  parentId: Id,
  version: Short,
  height: Long,
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
  mainChain: Boolean
)
