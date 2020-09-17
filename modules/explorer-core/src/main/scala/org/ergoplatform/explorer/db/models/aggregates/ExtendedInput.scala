package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.db.models.Input
import org.ergoplatform.explorer.{Address, TxId}

/** Input entity enriched with a data from corresponding output.
  */
final case class ExtendedInput(
  input: Input,
  value: Option[Long],
  outputTxId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address]
)
