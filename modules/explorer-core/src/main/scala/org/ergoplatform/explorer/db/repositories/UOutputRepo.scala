package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.UOutput

/** [[UOutput]] data access operations.
  */
trait UOutputRepo[D[_], S[_[_], _]] {

  /** Put a given unconfirmed `output` to persistence.
    */
  def insert(output: UOutput): D[Unit]

  /** Put a given list of unconfirmed outputs to persistence.
    */
  def insertMany(outputs: List[UOutput]): D[Unit]

  /** Get all outputs containing in unconfirmed transactions.
    */
  def getAll(offset: Int, limit: Int): S[D, UOutput]

  /** Get all unconfirmed outputs related to a given list of `txId`.
    */
  def getAllByTxIds(txsId: NonEmptyList[TxId]): D[List[UOutput]]
}
