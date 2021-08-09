package org.ergoplatform.explorer.db.repositories.bundles

import org.ergoplatform.explorer.db.repositories._

final case class UtxRepoBundle[D[_], S[_[_], _]](
  txs: UTransactionRepo[D, S],
  inputs: UInputRepo[D, S],
  dataInputs: UDataInputRepo[D, S],
  outputs: UOutputRepo[D, S],
  assets: UAssetRepo[D]
)
