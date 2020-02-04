package org.ergoplatform.explorer.db.models.aggregates

final case class DexSellOrderOutput(
  extOutput: ExtendedOutput,
  tokenPrice: Long
)
