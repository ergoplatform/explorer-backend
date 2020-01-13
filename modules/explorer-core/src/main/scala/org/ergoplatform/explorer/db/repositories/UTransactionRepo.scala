package org.ergoplatform.explorer.db.repositories

import org.ergoplatform.explorer.db.models.{UInput, UOutput, UTransaction}
import org.ergoplatform.explorer.{Address, HexString, TxId}

trait UTransactionRepo[D[_], S[_[_], _]] {

  /** Put a given `tx` to persistence.
    */
  def insert(tx: UTransaction): D[Unit]

  /** Put a given list of transactions to persistence.
    */
  def insertMany(txs: List[UTransaction]): D[Unit]

  /** Get unconfirmed transaction with a given `id`.
    */
  def get(id: TxId): D[Option[UTransaction]]

  /** Get all unconfirmed transactions related to the given `ergoTree`.
    */
  def getAllRelatedToErgoTree(
    ergoTree: HexString,
    offset: Int,
    limit: Int
  ): S[D, UTransaction]
}
