package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.protocol.models.ApiHeader
import org.ergoplatform.explorer.{HexString, Id}

/** Entity representing `node_headers` table.
  */
final case class Header(
  id: Id,
  parentId: Id,
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
  w: HexString,
  n: HexString,
  d: String,
  votes: String,
  mainChain: Boolean
)

object Header {

  def fromApi(apiHeader: ApiHeader): Header =
    Header(
      apiHeader.id,
      apiHeader.parentId,
      apiHeader.version,
      apiHeader.height,
      apiHeader.nBits,
      apiHeader.difficulty.value,
      apiHeader.timestamp,
      apiHeader.stateRoot,
      apiHeader.adProofsRoot,
      apiHeader.transactionsRoot,
      apiHeader.extensionHash,
      apiHeader.minerPk,
      apiHeader.w,
      apiHeader.n,
      apiHeader.d,
      apiHeader.votes,
      apiHeader.mainChain
    )
}
