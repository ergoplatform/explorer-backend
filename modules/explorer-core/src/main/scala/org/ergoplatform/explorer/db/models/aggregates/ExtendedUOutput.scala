package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.UOutput

/** Represents `node_u_outputs` table (Unconfirmed Output, which is a part of Unconfirmed Transaction).
  */
final case class ExtendedUOutput(
  output: UOutput,
  spendingTxId: Option[TxId]
)
