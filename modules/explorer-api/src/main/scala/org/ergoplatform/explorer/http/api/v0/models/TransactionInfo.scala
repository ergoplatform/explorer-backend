package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.db.models.{Asset, Transaction}
import org.ergoplatform.explorer.{Id, TxId}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class TransactionInfo(
  id: TxId,
  headerId: Id,
  timestamp: Long,
  confirmationsQty: Int,
  inputs: List[InputInfo],
  outputs: List[OutputInfo]
)

object TransactionInfo {

  implicit val codec: Codec[TransactionInfo] = deriveCodec

  implicit val schema: Schema[TransactionInfo] =
    implicitly[Derived[Schema[TransactionInfo]]].value
      .modify(_.id)(_.description("Transaction ID"))
      .modify(_.headerId)(
        _.description("ID of the block the transaction was included in")
      )
      .modify(_.timestamp)(
        _.description("Approx timestamp the transaction got into the network")
      )
      .modify(_.confirmationsQty)(_.description("Number of transaction confirmations"))

  def batch(
    txs: List[(Transaction, Int)],
    inputs: List[ExtendedInput],
    outputs: List[ExtendedOutput],
    assets: List[Asset]
  ): List[TransactionInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    txs.map {
      case (tx, numConfirmations) =>
        val relatedInputs = inputs
          .filter(_.input.txId == tx.id)
          .map(InputInfo.apply)
        val relatedOutputs = outputs.map { out =>
          OutputInfo(out, groupedAssets.get(out.output.boxId).toList.flatten)
        }
        val id = tx.id
        val ts = tx.timestamp
        apply(id, tx.headerId, ts, numConfirmations, relatedInputs, relatedOutputs)
    }
  }

  def apply(
    tx: Transaction,
    numConfirmations: Int,
    inputs: List[ExtendedInput],
    outputs: List[ExtendedOutput],
    assets: List[Asset]
  ): TransactionInfo = {
    val ins  = inputs.map(InputInfo.apply)
    val outs = OutputInfo.batch(outputs, assets)
    apply(tx.id, tx.headerId, tx.timestamp, numConfirmations, ins, outs)
  }
}
