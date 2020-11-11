package org.ergoplatform.explorer.db.models.aggregates

import java.util.concurrent.TimeUnit

import cats.Functor
import cats.effect.Clock
import cats.instances.try_._
import cats.syntax.functor._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.db.models.{UAsset, UDataInput, UInput, UOutput, UTransaction}
import org.ergoplatform.explorer.protocol.models.ApiTransaction
import org.ergoplatform.explorer.protocol.utils

import scala.util.Try

case class FlatUTransaction(
  tx: UTransaction,
  inputs: List[UInput],
  dataInputs: List[UDataInput],
  outputs: List[UOutput],
  assets: List[UAsset]
)

object FlatUTransaction {

  def fromApi[F[_]: Clock: Functor](
    apiTx: ApiTransaction
  )(implicit enc: ErgoAddressEncoder): F[FlatUTransaction] =
    Clock[F].realTime(TimeUnit.MILLISECONDS).map { ts =>
      val tx = UTransaction(apiTx.id, ts, apiTx.size)
      val ins = apiTx.inputs.zipWithIndex.map {
        case (apiIn, i) =>
          UInput(
            apiIn.boxId,
            apiTx.id,
            i,
            apiIn.spendingProof.proofBytes,
            apiIn.spendingProof.extension
          )
      }
      val dataIns = apiTx.dataInputs.zipWithIndex.map {
        case (apiIn, i) =>
          UDataInput(apiIn.boxId, apiTx.id, i)
      }
      val outs = apiTx.outputs.zipWithIndex.map {
        case (apiOut, idx) =>
          val addressOpt = utils
            .ergoTreeToAddress(apiOut.ergoTree)
            .map(_.toString)
            .flatMap(Address.fromString[Try])
            .toOption
          UOutput(
            apiOut.boxId,
            apiTx.id,
            apiOut.value,
            apiOut.creationHeight,
            idx,
            apiOut.ergoTree,
            addressOpt,
            ???
          )
      }
      val assets =
        apiTx.outputs
          .flatMap { o =>
            o.assets.zipWithIndex.map { case (asset, i) => (o.boxId, asset, i) }
          }
          .map {
            case (boxId, apiAsset, index) =>
              UAsset(apiAsset.tokenId, boxId, index, apiAsset.amount)
          }
      FlatUTransaction(tx, ins, dataIns, outs, assets)
    }
}
