package org.ergoplatform.explorer.db.models.aggregates

import cats.Monad
import cats.instances.try_._
import cats.syntax.functor._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{Address, CRaise}
import org.ergoplatform.explorer.Err.{ProcessingErr, RefinementFailed}
import org.ergoplatform.explorer.db.models._
import org.ergoplatform.explorer.protocol.models.{ApiBlockTransactions, ApiFullBlock}
import org.ergoplatform.explorer.protocol.utils
import org.ergoplatform.explorer.settings.ProtocolSettings

import scala.util.Try

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

  def fromApi[
    F[_]: CRaise[*[_], ProcessingErr]: CRaise[*[_], RefinementFailed]: Monad
  ](
    apiBlock: ApiFullBlock,
    parentInfoOpt: Option[BlockInfo]
  )(protocolSettings: ProtocolSettings): F[FlatBlock] =
    BlockInfo
      .fromApi[F](apiBlock, parentInfoOpt)(protocolSettings)
      .map { blockInfo =>
        implicit val e: ErgoAddressEncoder = protocolSettings.addressEncoder

        val outs = extractOutputs(
          apiBlock.transactions,
          apiBlock.header.mainChain,
          apiBlock.header.timestamp
        )
        val txs = extractTxs(
          apiBlock.transactions,
          apiBlock.header.timestamp,
          blockInfo.height,
          apiBlock.header.mainChain
        )
        val inputs = extractInputs(apiBlock.transactions, apiBlock.header.mainChain)
        val assets = extractAssets(apiBlock.transactions)
        FlatBlock(
          Header.fromApi(apiBlock.header),
          blockInfo,
          BlockExtension.fromApi(apiBlock.extension),
          apiBlock.adProofs.map(AdProof.fromApi),
          txs,
          inputs,
          outs,
          assets
        )
      }

  private def extractTxs(
    apiTxs: ApiBlockTransactions,
    ts: Long,
    height: Int,
    chainStatus: Boolean
  ): List[Transaction] = {
    val txs = apiTxs.transactions.zipWithIndex
    val coinbaseTxOpt = txs.lastOption
      .map {
        case (tx, i) =>
          Transaction(tx.id, apiTxs.headerId, height, isCoinbase = true, ts, tx.size, i, chainStatus)
      }
    val restTxs = txs.init
      .map {
        case (tx, i) =>
          Transaction(tx.id, apiTxs.headerId, height, isCoinbase = false, ts, tx.size, i, chainStatus)
      }
    restTxs ++ coinbaseTxOpt
  }

  private def extractInputs(
    apiTxs: ApiBlockTransactions,
    mainChain: Boolean
  ): List[Input] =
    apiTxs.transactions.flatMap { apiTx =>
      apiTx.inputs.map(i =>
        Input(
          i.boxId,
          apiTx.id,
          apiTxs.headerId,
          i.spendingProof.proofBytes,
          i.spendingProof.extension,
          mainChain
        )
      )
    }

  private def extractOutputs(
    apiTxs: ApiBlockTransactions,
    mainChain: Boolean,
    ts: Long
  )(implicit enc: ErgoAddressEncoder): List[Output] =
    apiTxs.transactions.flatMap { apiTx =>
      apiTx.outputs.zipWithIndex
        .map {
          case (o, index) =>
            val addressOpt = utils
              .ergoTreeToAddress(o.ergoTree)
              .map(_.toString)
              .flatMap(Address.fromString[Try])
              .toOption
            Output(
              o.boxId,
              apiTx.id,
              apiTxs.headerId,
              o.value,
              o.creationHeight,
              index,
              o.ergoTree,
              addressOpt,
              o.additionalRegisters,
              ts,
              mainChain
            )
        }
    }

  private def extractAssets(apiTxs: ApiBlockTransactions): List[Asset] =
    for {
      tx     <- apiTxs.transactions
      out    <- tx.outputs
      assets <- out.assets
    } yield Asset(assets.tokenId, out.boxId, apiTxs.headerId, assets.amount)
}
