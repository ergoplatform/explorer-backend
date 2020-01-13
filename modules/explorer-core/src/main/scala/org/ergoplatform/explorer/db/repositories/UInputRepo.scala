package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.UInput

/** [[UInput]] data access operations.
  */
trait UInputRepo[D[_], S[_[_], _]] {

  /** Put a given `input` to persistence.
    */
  def insert(input: UInput): D[Unit]

  /** Put a given list of inputs to persistence.
    */
  def insetMany(inputs: List[UInput]): D[Unit]

  /** Get all inputs containing in unconfirmed transactions.
   */
  def getAll(offset: Int, limit: Int): S[D, UInput]

  /** Get all inputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId]): D[List[UInput]]
}
