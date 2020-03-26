package org.ergoplatform.explorer.db.models.aggregates

import java.util.concurrent.TimeUnit

import cats.Functor
import cats.effect.Clock
import cats.instances.try_._
import cats.syntax.functor._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.db.models.{UAsset, UInput, UOutput, UTransaction}
import org.ergoplatform.explorer.protocol.models.ApiTransaction
import org.ergoplatform.explorer.protocol.utils

import scala.util.Try

case class FlatUTransaction(
  tx: UTransaction,
  inputs: List[UInput],
  outputs: List[UOutput],
  assets: List[UAsset]
)

object FlatUTransaction {

  def fromApi[F[_]: Clock: Functor](
    apiTx: ApiTransaction
  )(implicit enc: ErgoAddressEncoder): F[FlatUTransaction] =
    Clock[F].realTime(TimeUnit.NANOSECONDS).map { ts =>
      val tx = UTransaction(apiTx.id, ts, apiTx.size)
      val ins = apiTx.inputs.map { apiIn =>
        UInput(
          apiIn.boxId,
          apiTx.id,
          apiIn.spendingProof.proofBytes,
          apiIn.spendingProof.extension
        )
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
            apiOut.additionalRegisters
          )
      }
      val assets = apiTx.outputs.flatMap(o => o.assets.map(o.boxId -> _)).map {
        case (boxId, apiAsset) =>
          UAsset(apiAsset.tokenId, boxId, apiAsset.amount)
      }
      FlatUTransaction(tx, ins, outs, assets)
    }
}
