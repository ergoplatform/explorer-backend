package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.db.models.UDataInput
import org.ergoplatform.explorer.{Address, TxId}

/** Unconfirmed input entity enriched with a data from corresponding output.
 */
final case class ExtendedUDataInput(
  input: UDataInput,
  value: Option[Long],
  outputTxId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address]
)
