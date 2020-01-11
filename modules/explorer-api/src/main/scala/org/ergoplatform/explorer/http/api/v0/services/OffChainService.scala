package org.ergoplatform.explorer.http.api.v0.services

import org.ergoplatform.explorer.{Address, HexString, TxId}
import org.ergoplatform.explorer.protocol.models.{ApiInput, ApiOutput, ApiTransaction}

/** A service providing an access to unconfirmed transactions data.
  */
trait OffChainService[F[_]] {

  /** Get unconfirmed transaction with a given `id`.
    */
  def getUnconfirmedTxById(id: TxId): F[Option[ApiTransaction]]

  /** Get all unconfirmed transactions related to the given `address`.
    */
  def getUnconfirmedTxsByAddress(address: Address): F[List[ApiTransaction]]

  /** Get all unconfirmed transactions related to the given `ergoTree`.
    */
  def getUnconfirmedTxsByErgoTree(ergoTree: HexString): F[List[ApiTransaction]]

  /** Get all outputs containing in unconfirmed transactions.
    */
  def getAllUnconfirmedOutputs: F[List[ApiOutput]]

  /** Get all inputs containing in unconfirmed transactions.
    */
  def getAllUnconfirmedInputs: F[List[ApiInput]]
}
