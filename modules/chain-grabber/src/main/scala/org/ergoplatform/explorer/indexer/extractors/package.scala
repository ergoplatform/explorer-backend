package org.ergoplatform.explorer.indexer

import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import cats.{Applicative, Monad}
import io.circe.syntax._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.models._
import org.ergoplatform.explorer.indexer.models.SlotData
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, RegisterValue}
import org.ergoplatform.explorer.protocol.{registers, sigma, RegistersParser}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.explorer.{Address, BuildFrom}
import tofu.syntax.context._
import tofu.syntax.monadic._
import tofu.{Throws, WithContext}

import scala.util.Try

package object extractors {

  implicit def headerBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, Header] =
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
        mainChain = false
      )
    }

  implicit def blockInfoBuildFrom[
    F[_]: Monad: WithContext[*[_], ProtocolSettings]: Throws
  ]: BuildFrom[F, SlotData, BlockStats] =
    new BlockInfoBuildFrom

  implicit def blockExtensionBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, BlockExtension] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, _, apiExtension, _, _), _) =>
      BlockExtension(
        apiExtension.headerId,
        apiExtension.digest,
        apiExtension.fields
      )
    }

  implicit def adProofBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, Option[AdProof]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, _, _, apiAdProofOpt, _), _) =>
      apiAdProofOpt.map { apiAdProof =>
        AdProof(
          apiAdProof.headerId,
          apiAdProof.proofBytes,
          apiAdProof.digest
        )
      }
    }

  implicit def txsBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, List[Transaction]] =
    BuildFrom.pure { case SlotData(apiBlock, parentOpt) =>
      val lastTxGlobalIndex = parentOpt.map(_.maxTxGix).getOrElse(-1L)
      val headerId          = apiBlock.header.id
      val height            = apiBlock.header.height
      val ts                = apiBlock.header.timestamp
      val txs =
        apiBlock.transactions.transactions.zipWithIndex
          .map { case (tx, i) =>
            val globalIndex = lastTxGlobalIndex + i + 1
            Transaction(tx.id, headerId, height, isCoinbase = false, ts, tx.size, i, globalIndex, mainChain = false)
          }
      val (init, coinbase) = txs.init -> txs.lastOption
      init ++ coinbase.map(_.copy(isCoinbase = true))
    }

  implicit def inputsBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, List[Input]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, apiTxs, _, _, _), _) =>
      apiTxs.transactions.flatMap { apiTx =>
        apiTx.inputs.toList.zipWithIndex.map { case (i, index) =>
          Input(
            i.boxId,
            apiTx.id,
            apiTxs.headerId,
            i.spendingProof.proofBytes,
            i.spendingProof.extension,
            index,
            mainChain = false
          )
        }
      }
    }

  implicit def dataInputsBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, List[DataInput]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, apiTxs, _, _, _), _) =>
      apiTxs.transactions.flatMap { apiTx =>
        apiTx.dataInputs.zipWithIndex.map { case (i, index) =>
          DataInput(
            i.boxId,
            apiTx.id,
            apiTxs.headerId,
            index,
            mainChain = false
          )
        }
      }
    }

  implicit def outputsBuildFrom[F[_]: Monad: Throws: WithContext[*[_], ProtocolSettings]]
    : BuildFrom[F, SlotData, List[Output]] =
    BuildFrom.instance { case SlotData(ApiFullBlock(header, apiTxs, _, _, _), parentOpt) =>
      val lastOutputGlobalIndex = parentOpt.map(_.maxBoxGix).getOrElse(-1L)
      context.flatMap { protocolSettings =>
        implicit val e: ErgoAddressEncoder = protocolSettings.addressEncoder
        apiTxs.transactions.zipWithIndex
          .flatMap { case (tx, tix) =>
            tx.outputs.toList.zipWithIndex
              .map { case (o, oix) => ((o, tx.id), oix, tix) }
          }
          .sortBy { case (_, oix, tix) => (tix, oix) }
          .map { case ((o, txId), oix, _) => (o, oix, txId) }
          .zipWithIndex
          .traverse { case ((o, outIndex, txId), blockIndex) =>
            for {
              address <- sigma
                           .ergoTreeToAddress[F](o.ergoTree)
                           .map(_.toString)
                           .flatMap(Address.fromString[F])
              scriptTemplateHash <- sigma.deriveErgoTreeTemplateHash[F](o.ergoTree)
              registersJson = registers.expand(o.additionalRegisters).asJson
              globalIndex   = lastOutputGlobalIndex + blockIndex + 1
            } yield Output(
              o.boxId,
              txId,
              apiTxs.headerId,
              o.value,
              o.creationHeight,
              header.height,
              outIndex,
              globalIndex,
              o.ergoTree,
              scriptTemplateHash,
              address,
              registersJson,
              header.timestamp,
              mainChain = false
            )
          }
      }
    }

  implicit def assetsBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, List[Asset]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, apiTxs, _, _, _), _) =>
      for {
        tx             <- apiTxs.transactions
        out            <- tx.outputs.toList
        (asset, index) <- out.assets.zipWithIndex
      } yield Asset(asset.tokenId, out.boxId, apiTxs.headerId, index, asset.amount)
    }

  implicit def registersBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, List[BoxRegister]] =
    BuildFrom.pure { case SlotData(ApiFullBlock(_, apiTxs, _, _, _), _) =>
      for {
        tx                            <- apiTxs.transactions
        out                           <- tx.outputs.toList
        (id, rawValue)                <- out.additionalRegisters.toList
        RegisterValue(typeSig, value) <- RegistersParser[Try].parseAny(rawValue).toOption
      } yield BoxRegister(id, out.boxId, typeSig, rawValue, value)
    }

//  implicit def scriptConstantsBuildFrom[F[_]: Monad]: BuildFrom[F, SlotData, List[ScriptConstant]] =
//    BuildFrom.pure { case SlotData(ApiFullBlock(_, apiTxs, _, _, _), _) =>
//      for {
//        tx        <- apiTxs.transactions
//        out       <- tx.outputs.toList
//        constants <- sigma.extractErgoTreeConstants[Try](out.ergoTree).toOption.toList
//        (ix, tp, v, rv) <- constants.flatMap { case (ix, c, v) =>
//                             sigma.renderEvaluatedValue(c).map { case (tp, rv) => (ix, tp, v, rv) }.toList
//                           }
//      } yield ScriptConstant(ix, out.boxId, tp, v, rv)
//    }

  implicit def scriptConstantsBuildFrom[F[_]: Monad]: BuildFrom[F, SlotData, List[ScriptConstant]] =
    BuildFrom.pure(_ => List.empty)

  implicit def tokensBuildFrom[F[_]: Applicative]: BuildFrom[F, SlotData, List[Token]] =
    new TokensBuildFromEip4
}
