package org.ergoplatform.explorer.persistence.models

import org.ergoplatform.explorer.{Id, TxId}

final case class Transaction(
  id: TxId,
  headerId: Id,
  isCoinbase: Boolean,
  timestamp: Long,
  size: Int
)
