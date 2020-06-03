package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.{Address, TxId}
import org.ergoplatform.explorer.db.models.UInput

/** Unconfirmed input entity enriched with a data from corresponding output.
 */
final case class ExtendedUInput(
  input: UInput,
  value: Option[Long],
  outputTxId: Option[TxId],
  address: Option[Address]
)
