package org.ergoplatform.explorer.http.api.v0.models

import cats.instances.list._
import cats.instances.option._
import cats.syntax.traverse._
import cats.syntax.option._
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.db.models.{UAsset, UInput, UOutput, UTransaction}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

// TODO ScalaDoc
final case class UTransactionInfo(
  id: TxId,
  inputs: List[UInputInfo],
  outputs: List[UOutputInfo],
  creationTimestamp: Long, // TODO ScalaDoc: does it correspond to anything in Ergo?
  size: Int  // TODO ScalaDoc: which size?
)

object UTransactionInfo {

  implicit val codec: Codec[UTransactionInfo] = deriveCodec

  implicit val schema: Schema[UTransactionInfo] =
    implicitly[Derived[Schema[UTransactionInfo]]].value
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.creationTimestamp)(
        _.description("Approximate time this transaction appeared in the network")
      )
      .modify(_.size)(
        _.description("Size of the transaction in bytes")
      )

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
