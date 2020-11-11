package org.ergoplatform.explorer.db.models.aggregates

import cats.Monad
import cats.instances.try_._
import cats.syntax.functor._
import io.circe.syntax._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Err.{ProcessingErr, RefinementFailed}
import org.ergoplatform.explorer.db.models._
import org.ergoplatform.explorer.protocol.models.{ApiBlockTransactions, ApiFullBlock, ExpandedRegister, RegisterValue}
import org.ergoplatform.explorer.protocol.{utils, RegistersParser}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.explorer.{Address, CRaise}

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
  dataInputs: List[DataInput],
  outputs: List[Output],
  assets: List[Asset],
  registers: List[BoxRegister]
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
        val mainChain                      = apiBlock.header.mainChain
        val outs = extractOutputs(
          apiBlock.transactions,
          mainChain,
          apiBlock.header.timestamp
        )
        val txs = extractTxs(
          apiBlock.transactions,
          apiBlock.header.timestamp,
          blockInfo.height,
          mainChain
        )
        val inputs     = extractInputs(apiBlock.transactions, mainChain)
        val dataInputs = extractDataInputs(apiBlock.transactions, mainChain)
        val assets     = extractAssets(apiBlock.transactions)
        val registers  = extractRegisters(apiBlock.transactions)
        FlatBlock(
          Header.fromApi(apiBlock.header),
          blockInfo,
          BlockExtension.fromApi(apiBlock.extension),
          apiBlock.adProofs.map(AdProof.fromApi),
          txs,
          inputs,
          dataInputs,
          outs,
          assets,
          registers
        )
      }

  private def extractTxs(
    apiTxs: ApiBlockTransactions,
    ts: Long,
    height: Int,
    mainChain: Boolean
  ): List[Transaction] = {
    val txs = apiTxs.transactions.zipWithIndex
    val coinbaseTxOpt = txs.lastOption
      .map {
        case (tx, i) =>
          Transaction(tx.id, apiTxs.headerId, height, isCoinbase = true, ts, tx.size, i, mainChain)
      }
    val restTxs = txs.init
      .map {
        case (tx, i) =>
          Transaction(tx.id, apiTxs.headerId, height, isCoinbase = false, ts, tx.size, i, mainChain)
      }
    restTxs ++ coinbaseTxOpt
  }

  private def extractInputs(
    apiTxs: ApiBlockTransactions,
    mainChain: Boolean
  ): List[Input] =
    apiTxs.transactions.flatMap { apiTx =>
      apiTx.inputs.zipWithIndex.map {
        case (i, index) =>
          Input(
            i.boxId,
            apiTx.id,
            apiTxs.headerId,
            i.spendingProof.proofBytes,
            i.spendingProof.extension,
            index,
            mainChain
          )
      }
    }

  private def extractDataInputs(
    apiTxs: ApiBlockTransactions,
    mainChain: Boolean
  ): List[DataInput] =
    apiTxs.transactions.flatMap { apiTx =>
      apiTx.dataInputs.zipWithIndex.map {
        case (i, index) =>
          DataInput(
            i.boxId,
            apiTx.id,
            apiTxs.headerId,
            index,
            mainChain
          )
      }
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
            val registers =
              for {
                (idSig, rawValue)               <- o.additionalRegisters.toList
                RegisterValue(valueType, value) <- RegistersParser[Try].parse(rawValue).toOption
              } yield idSig.entryName -> ExpandedRegister(rawValue, valueType, value)
            val registersJson = registers.toMap.asJson
            Output(
              o.boxId,
              apiTx.id,
              apiTxs.headerId,
              o.value,
              o.creationHeight,
              index,
              o.ergoTree,
              addressOpt,
              registersJson,
              ts,
              mainChain
            )
        }
    }

  private def extractAssets(apiTxs: ApiBlockTransactions): List[Asset] =
    for {
      tx             <- apiTxs.transactions
      out            <- tx.outputs
      (asset, index) <- out.assets.zipWithIndex
    } yield Asset(asset.tokenId, out.boxId, apiTxs.headerId, index, asset.amount)

  private def extractRegisters(apiTxs: ApiBlockTransactions): List[BoxRegister] =
    for {
      tx                            <- apiTxs.transactions
      out                           <- tx.outputs
      (idSig, rawValue)             <- out.additionalRegisters.toList
      RegisterValue(typeSig, value) <- RegistersParser[Try].parse(rawValue).toOption
    } yield BoxRegister(idSig.id, out.boxId, apiTxs.headerId, typeSig, rawValue, value)
}
