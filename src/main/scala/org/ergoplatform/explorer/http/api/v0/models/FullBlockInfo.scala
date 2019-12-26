package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.db.models.BlockExtension

final case class FullBlockInfo(
  headerInfo: HeaderInfo,
  transactionsInfo: List[TransactionInfo],
  extension: BlockExtension,
  adProof: Option[AdProofInfo]
)
