package org.ergoplatform.explorer.grabber

import cats.{Applicative, FlatMap, Monad}
import io.circe.syntax._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.db.models._
import org.ergoplatform.explorer.grabber.models.SlotData
import org.ergoplatform.explorer.grabber.modules.BuildFrom
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, RegisterValue}
import org.ergoplatform.explorer.protocol.{RegistersParser, registers, utils}
import org.ergoplatform.explorer.settings.ProtocolSettings
import tofu.syntax.context._
import tofu.syntax.monadic._
import tofu.{Throws, WithContext}

import scala.util.Try

package object extractors {

  implicit def headerExtract[F[_]: Applicative]: BuildFrom[F, SlotData, Header] =
    BuildFrom.pure { case SlotData(ApiFullBlock(apiHeader, _, _, _, _), _) =>
      Header(
        apiHeader.id,
        apiHeader.parentId,
        apiHeader.version,
        apiHeader.height,
        apiHeader.nBits,
        apiHeader.difficulty.value,
        apiHeader.timestamp,
        apiHeader.stateRoot,
        apiHeader.adProofsRoot,
        apiHeader.transactionsRoot,
        apiHeader.extensionHash,
        apiHeader.minerPk,
        apiHeader.w,
        apiHeader.n,
        apiHeader.d,
        apiHeader.votes,
        apiHeader.mainChain
      )
    }

  implicit def blockInfoExtract[
    F[_]: Monad: WithContext[*[_], ProtocolSettings]: Throws
  ]: BuildFrom[F, SlotData, BlockInfo] =
    new BlockInfoBuildFrom[F]

  implicit def blockExtensionExtract[F[_]: Applicative]: BuildFrom[F, SlotData, BlockExtension] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, _, apiExtension, _, _), _) =>
      BlockExtension(
        apiExtension.headerId,
        apiExtension.digest,
        apiExtension.fields
      )
    }

  implicit def adProofExtract[F[_]: Applicative]: BuildFrom[F, SlotData, Option[AdProof]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, _, _, apiAdProofOpt, _), _) =>
      apiAdProofOpt.map { apiAdProof =>
        AdProof(
          apiAdProof.headerId,
          apiAdProof.proofBytes,
          apiAdProof.digest
        )
      }
    }

  implicit def txsExtract[F[_]: Applicative]: BuildFrom[F, SlotData, List[Transaction]] =
    BuildFrom.pure { case SlotData(apiBlock, _) =>
      val headerId  = apiBlock.header.id
      val height    = apiBlock.header.height
      val mainChain = apiBlock.header.mainChain
      val ts        = apiBlock.header.timestamp
      val txs       = apiBlock.transactions.transactions.zipWithIndex
      val coinbaseTxOpt = txs.lastOption
        .map { case (tx, i) =>
          Transaction(tx.id, headerId, height, isCoinbase = true, ts, tx.size, i, mainChain)
        }
      val restTxs = txs.init
        .map { case (tx, i) =>
          Transaction(tx.id, headerId, height, isCoinbase = false, ts, tx.size, i, mainChain)
        }
      restTxs ++ coinbaseTxOpt
    }

  implicit def inputsExtract[F[_]: Applicative]: BuildFrom[F, SlotData, List[Input]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(header, apiTxs, _, _, _), _) =>
      apiTxs.transactions.flatMap { apiTx =>
        apiTx.inputs.zipWithIndex.map { case (i, index) =>
          Input(
            i.boxId,
            apiTx.id,
            apiTxs.headerId,
            i.spendingProof.proofBytes,
            i.spendingProof.extension,
            index,
            header.mainChain
          )
        }
      }
    }

  implicit def dataInputsExtract[F[_]: Applicative]: BuildFrom[F, SlotData, List[DataInput]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(header, apiTxs, _, _, _), _) =>
      apiTxs.transactions.flatMap { apiTx =>
        apiTx.dataInputs.zipWithIndex.map { case (i, index) =>
          DataInput(
            i.boxId,
            apiTx.id,
            apiTxs.headerId,
            index,
            header.mainChain
          )
        }
      }
    }

  implicit def outputsExtract[F[_]: FlatMap: WithContext[*[_], ProtocolSettings]]: BuildFrom[F, SlotData, List[Output]] =
    BuildFrom.instance { case SlotData(ApiFullBlock(header, apiTxs, _, _, _), _) =>
      context.map { protocolSettings =>
        implicit val e: ErgoAddressEncoder = protocolSettings.addressEncoder
        apiTxs.transactions.flatMap { apiTx =>
          apiTx.outputs.zipWithIndex
            .map { case (o, index) =>
              val addressOpt = utils
                .ergoTreeToAddress(o.ergoTree)
                .map(_.toString)
                .flatMap(Address.fromString[Try])
                .toOption
              val registersJson = registers.expand(o.additionalRegisters).asJson
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
                header.timestamp,
                header.mainChain
              )
            }
        }
      }
    }

  implicit def assetsExtract[F[_]: Applicative]: BuildFrom[F, SlotData, List[Asset]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, apiTxs, _, _, _), _) =>
      for {
        tx             <- apiTxs.transactions
        out            <- tx.outputs
        (asset, index) <- out.assets.zipWithIndex
      } yield Asset(asset.tokenId, out.boxId, apiTxs.headerId, index, asset.amount)
    }

  implicit def registersExtract[F[_]: Applicative]: BuildFrom[F, SlotData, List[BoxRegister]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, apiTxs, _, _, _), _) =>
      for {
        tx                            <- apiTxs.transactions
        out                           <- tx.outputs
        (id, rawValue)                <- out.additionalRegisters.toList
        RegisterValue(typeSig, value) <- RegistersParser[Try].parse(rawValue).toOption
      } yield BoxRegister(id, out.boxId, apiTxs.headerId, typeSig, rawValue, value)
    }
}
