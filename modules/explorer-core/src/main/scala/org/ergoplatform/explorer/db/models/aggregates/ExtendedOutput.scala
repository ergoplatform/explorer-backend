package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.Output

/** Output entity enriched with a data from corresponding transaction.
  */
final case class ExtendedOutput(
  output: Output,
  spentByOpt: Option[TxId]
)
