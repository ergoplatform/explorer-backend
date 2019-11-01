package org.ergoplatform.explorer.persistence.models

final case class Transaction(
  id: String,
  headerId: String,
  isCoinbase: Boolean,
  timestamp: Long,
  size: Long
)
