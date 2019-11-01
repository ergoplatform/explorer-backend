package org.ergoplatform.explorer.persistence.models.composite

import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.persistence.models.Output

final case class ExtendedOutput(
  output: Output,
  spentByOpt: Option[TxId],
  mainChain: Boolean
)
