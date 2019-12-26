package org.ergoplatform.explorer.http.api.v0.models

final case class TransactionInfo(
  id: String,
  headerId: String,
  timestamp: Long,
  confirmationsQty: Long,
  inputs: List[InputInfo],
  outputs: List[OutputInfo]
)
