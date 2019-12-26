package org.ergoplatform.explorer.http.api.v0.models

final case class InputInfo(
  id: String,
  proof: String,
  value: Option[Long],
  txId: String,
  outputTransactionId: Option[String],
  address: Option[String]
)
