package org.ergoplatform.explorer.http.api.v0.services

import cats.syntax.list._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.instances.try_._
import cats.{~>, Applicative, Monad}
import fs2.Stream
import org.ergoplatform.explorer.Err.RequestProcessingErr
import org.ergoplatform.explorer.Err.RequestProcessingErr.{
  AddressDecodingFailed,
  DexBuyOrderAttributesFailed,
  DexSellOrderAttributesFailed,
  InconsistentDbData
}
import org.ergoplatform.explorer.{ContractAttributes, HexString, TokenId}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import org.ergoplatform.explorer.syntax.stream._
import scorex.util.encode.Base16
import sigmastate.{SLong, Values}
import sigmastate.Values.{ByteArrayConstant, ErgoTree}
import sigmastate.serialization.ErgoTreeSerializer
import tofu.Raise.ContravariantRaise
import tofu.syntax.raise._

import scala.util.Try

/** A service providing an access to the DEX data.
  */
trait DexService[F[_], S[_[_], _]] {

  def getUnspentSellOrders(tokenId: TokenId): S[F, OutputInfo]

  def getUnspentBuyOrders(tokenId: TokenId): S[F, OutputInfo]
}

object DexService {

  def apply[
    F[_],
    D[_]: LiftConnectionIO: ContravariantRaise[*[_], DexSellOrderAttributesFailed]: ContravariantRaise[
      *[_],
      DexBuyOrderAttributesFailed
    ]: Monad
  ](
    xa: D ~> F
  ): DexService[F, Stream] =
    new Live(AssetRepo[D], DexOrdersRepo[D])(xa)

  final private class Live[
    F[_],
    D[_]: ContravariantRaise[*[_], DexSellOrderAttributesFailed]: ContravariantRaise[*[_], DexBuyOrderAttributesFailed]: Monad
  ](
    assetRepo: AssetRepo[D, Stream],
    dexOrdersRepo: DexOrdersRepo[D, Stream]
  )(xa: D ~> F)
    extends DexService[F, Stream] {

    private val sellContractTemplate: HexString = HexString
      .fromString[Try](
        "eb027300d1eded91b1a57301e6c6b2a5730200040ed801d60193e4c6b2a5730300040ec5a7eded92c1b2a57304007305720193c2b2a5730600d07307"
      )
      .get

    private val buyContractTemplate: HexString = HexString
      .fromString[Try](
        "eb027300d1eded91b1a57301e6c6b2a5730200040ed803d601e4c6b2a5730300020c4d0ed602eded91b172017304938cb27201730500017306928cb27201730700027308d60393e4c6b2a5730900040ec5a7eded720293c2b2a5730a00d0730b7203"
      )
      .get

    private val treeSerializer: ErgoTreeSerializer = new ErgoTreeSerializer

    // TODO: extract?
    @inline def sellContractAttributes[
      F[_]: ContravariantRaise[*[_], DexSellOrderAttributesFailed]: Applicative
    ](ergoTree: HexString): F[ContractAttributes] =
      Base16
        .decode(ergoTree.unwrapped)
        .toOption
        .flatMap { bytes =>
          val tree = treeSerializer.deserializeErgoTree(bytes)
          tree.constants.lift(5).collect {
            case Values.ConstantNode(value, SLong) =>
              ContractAttributes(Map("tokenPrice" -> value.asInstanceOf[Long].toString))
          }
        }
        .orRaise[F](
          DexSellOrderAttributesFailed(
            s"Cannot extract tokenPrice from sell order ergo tree $ergoTree"
          )
        )

    @inline def buyContractAttributes[
      F[_]: ContravariantRaise[*[_], DexBuyOrderAttributesFailed]: Applicative
    ](ergoTree: HexString): F[ContractAttributes] =
      Base16
        .decode(ergoTree.unwrapped)
        .toOption
        .flatMap { bytes =>
          val tree = treeSerializer.deserializeErgoTree(bytes)
          for {
            tokenId <- tree.constants.lift(6).collect {
                        case ByteArrayConstant(coll) => coll.toArray
                      }
            tokenAmount <- tree.constants.lift(8).collect {
                            case Values.ConstantNode(value, SLong) =>
                              value.asInstanceOf[Long]
                          }

          } yield ContractAttributes(
            Map(
              "tokenId"     -> Base16.encode(tokenId),
              "tokenAmount" -> tokenAmount.toString
            )
          )
        }
        .orRaise[F](
          DexBuyOrderAttributesFailed(
            s"Cannot extract token info from buy order ergo tree $ergoTree"
          )
        )

    override def getUnspentSellOrders(tokenId: TokenId): Stream[F, OutputInfo] =
      (
        for {
          sellOrder <- dexOrdersRepo
                        .getAllMainUnspentSellOrderByTokenId(
                          tokenId,
                          sellContractTemplate
                        )
          assets        <- assetRepo.getAllByBoxId(sellOrder.output.boxId).asStream
          contractAttrs <- sellContractAttributes(sellOrder.output.ergoTree).asStream
        } yield OutputInfo(
          sellOrder,
          assets,
          Some(contractAttrs)
        )
      ).translate(xa)

    override def getUnspentBuyOrders(tokenId: TokenId): Stream[F, OutputInfo] =
      (for {
        buyOrder <- dexOrdersRepo
                     .getAllMainUnspentBuyOrderByTokenId(tokenId, buyContractTemplate)
        assets        <- assetRepo.getAllByBoxId(buyOrder.output.boxId).asStream
        contractAttrs <- buyContractAttributes(buyOrder.output.ergoTree).asStream
      } yield OutputInfo(
        buyOrder,
        assets,
        Some(contractAttrs)
      )).translate(xa)

  }
}
