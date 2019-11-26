package org.ergoplatform.explorer.db.models.composite

import java.util.concurrent.TimeUnit

import cats.effect.Clock
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.{ApplicativeError, MonadError}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.models._
import org.ergoplatform.explorer.protocol.models.{ApiBlockTransactions, ApiFullBlock}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.explorer.{utils, Address}

/** Flattened representation of a full block from
  * Ergo protocol enriched with statistics.
  */
final case class FlatBlock(
  header: Header,
  info: BlockInfo,
  extension: BlockExtension,
  adProofOpt: Option[AdProof],
  txs: List[Transaction],
  inputs: List[Input],
  outputs: List[Output],
  assets: List[Asset]
)

object FlatBlock {

  def fromApi[F[_]: MonadError[*[_], Throwable]: Clock](
    apiBlock: ApiFullBlock,
    parentInfoOpt: Option[BlockInfo]
  )(protocolSettings: ProtocolSettings): F[FlatBlock] =
    BlockInfo
      .fromApi(apiBlock, parentInfoOpt)(protocolSettings)
      .flatMap { blockInfo =>
        Clock[F].realTime(TimeUnit.NANOSECONDS).flatMap { ts =>
          implicit val e: ErgoAddressEncoder = protocolSettings.addressEncoder
          extractOutputs(apiBlock.transactions, apiBlock.header.mainChain, ts)
            .map { outputs =>
              val txs    = extractTxs(apiBlock.transactions, ts)
              val inputs = extractInputs(apiBlock.transactions, apiBlock.header.mainChain)
              val assets = extractAssets(apiBlock.transactions)
              FlatBlock(
                Header.fromApi(apiBlock.header),
                blockInfo,
                BlockExtension.fromApi(apiBlock.extension),
                apiBlock.adProofs.map(AdProof.fromApi),
                txs,
                inputs,
                outputs,
                assets
              )
            }
        }
      }

  private def extractTxs(apiTxs: ApiBlockTransactions, ts: Long): List[Transaction] = {
    val txs        = apiTxs.transactions
    val coinbaseId = txs.last.id
    val coinbaseTx =
      Transaction(coinbaseId, apiTxs.headerId, isCoinbase = true, ts, txs.last.size)
    val restTxs = txs.init.map { tx =>
      Transaction(tx.id, apiTxs.headerId, isCoinbase = false, ts, tx.size)
    }
    coinbaseTx :: restTxs
  }

  private def extractInputs(
    apiTxs: ApiBlockTransactions,
    mainChain: Boolean
  ): List[Input] =
    apiTxs.transactions.flatMap { apiTx =>
      apiTx.inputs.map(
        i =>
          Input(
            i.boxId,
            apiTx.id,
            i.spendingProof.proofBytes,
            i.spendingProof.extension,
            mainChain
        )
      )
    }

  private def extractOutputs[F[_]: ApplicativeError[*[_], Throwable]](
    apiTxs: ApiBlockTransactions,
    mainChain: Boolean,
    ts: Long
  )(implicit enc: ErgoAddressEncoder): F[List[Output]] =
    apiTxs.transactions.flatMap { apiTx =>
      apiTx.outputs.zipWithIndex
        .map {
          case (o, index) =>
            val address: String = utils
              .ergoTreeToAddress(o.ergoTree.unwrapped)
              .map(_.toString)
              .getOrElse("cannot derive address")
            Address
              .fromString(address)
              .map { address =>
                Output(
                  o.boxId,
                  apiTx.id,
                  o.value,
                  o.creationHeight,
                  index,
                  o.ergoTree,
                  address,
                  o.additionalRegisters,
                  ts,
                  mainChain
                )
              }
        }
    }.sequence

  private def extractAssets(apiTxs: ApiBlockTransactions): List[Asset] =
    for {
      tx     <- apiTxs.transactions
      out    <- tx.outputs
      assets <- out.assets
    } yield Asset(assets.tokenId, out.boxId, assets.amount)
}
