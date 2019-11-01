package org.ergoplatform.explorer.persistence.models.composite

import org.ergoplatform.explorer.persistence.models.Input
import org.ergoplatform.explorer.{Address, TxId}

final case class ExtendedInput(
  input: Input,
  value: Option[Long],
  outputTxId: Option[TxId],
  address: Option[Address]
)
