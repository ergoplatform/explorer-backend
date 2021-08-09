package org.ergoplatform.explorer.db.repositories.bundles

import org.ergoplatform.explorer.cache.repositories.ErgoLikeTransactionRepo
import org.ergoplatform.explorer.db.repositories._

final case class UtxRepoBundle[F[_], D[_], S[_[_], _]](
  txs: UTransactionRepo[D, S],
  inputs: UInputRepo[D, S],
  dataInputs: UDataInputRepo[D, S],
  outputs: UOutputRepo[D, S],
  confirmedOutputs: OutputRepo[D, S],
  assets: UAssetRepo[D],
  ergoTxRepo: Option[ErgoLikeTransactionRepo[F, S]]
)
