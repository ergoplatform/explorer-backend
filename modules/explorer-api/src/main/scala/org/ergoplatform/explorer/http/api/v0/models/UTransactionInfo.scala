package org.ergoplatform.explorer.http.api.v0.models

import cats.instances.list._
import cats.instances.option._
import cats.syntax.traverse._
import cats.syntax.option._
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.{UAsset, UInput, UOutput, UTransaction}

final case class UTransactionInfo(
  id: TxId,
  inputs: List[UInputInfo],
  outputs: List[UOutputInfo],
  creationTimestamp: Long,
  size: Int
)

object UTransactionInfo {

  def apply(
    tx: UTransaction,
    ins: List[UInput],
    outs: List[UOutput],
    assets: List[UAsset]
  ): UTransactionInfo = {
    val inputsInfo  = ins.map(UInputInfo.apply)
    val outputsInfo = outs.map(UOutputInfo(_, assets))
    new UTransactionInfo(tx.id, inputsInfo, outputsInfo, tx.creationTimestamp, tx.size)
  }

  def batch(
    txs: List[UTransaction],
    ins: List[UInput],
    outs: List[UOutput],
    assets: List[UAsset]
  ): List[UTransactionInfo] = {
    val assetsByBox = assets.groupBy(_.boxId)
    val inputsByTx  = ins.groupBy(_.txId)
    val outsByTx    = outs.groupBy(_.txId)
    txs
      .traverse { tx =>
        for {
          inputs  <- inputsByTx.get(tx.id)
          outputs <- outsByTx.get(tx.id)
          assets  <- outputs
                       .foldLeft(List.empty[UAsset]) { (acc, o) =>
                         assetsByBox.get(o.boxId).toList.flatten ++ acc
                       }
                       .some
        } yield apply(tx, inputs, outputs, assets)
      }
      .toList
      .flatten
  }
}
