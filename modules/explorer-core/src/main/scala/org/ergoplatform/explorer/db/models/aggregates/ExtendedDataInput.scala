package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.db.models.DataInput
import org.ergoplatform.explorer.{Address, TxId}

/** DataInput entity enriched with a data from corresponding output.
  */
final case class ExtendedDataInput(
  input: DataInput,
  value: Option[Long],
  outputTxId: Option[TxId],
  address: Option[Address]
)
