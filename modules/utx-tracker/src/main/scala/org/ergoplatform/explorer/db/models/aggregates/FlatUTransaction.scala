package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.db.models._

final case class FlatUTransaction(
  tx: UTransaction,
  inputs: List[UInput],
  dataInputs: List[UDataInput],
  outputs: List[UOutput],
  assets: List[UAsset]
)
