package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.protocol.models.ApiHeader
import org.ergoplatform.explorer.{HexString, Id}

/** Represents `node_headers` table.
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
  w: HexString,                // PoW one time PK
  n: HexString,                // PoW nonce
  d: String,                   // PoW distance
  votes: String,               // hex-encoded votes for a soft-fork and parameters
  mainChain: Boolean           // chain status, `true` if this header resides in main chain.
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
