package org.ergoplatform.explorer.protocol

import cats.{Applicative, FlatMap, Monad}
import cats.syntax.flatMap._
import cats.syntax.either._
import org.ergoplatform.contracts.AssetsAtomicExchangeCompilation
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.ContractParsingErr.Base16DecodingFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.{
  DexBuyOrderAttributesFailed,
  DexContractInstantiationFailed,
  DexSellOrderAttributesFailed
}
import org.ergoplatform.explorer.protocol.utils.{bytesToErgoTree, ergoTreeTemplateBytes}
import org.ergoplatform.explorer.{CRaise, HexString, TokenId}
import org.ergoplatform.sigma.verified.VerifiedTypeConverters._
import scorex.util.encode.Base16
import sigmastate.Values.{ByteArrayConstant, ErgoTree}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.Extensions._
import sigmastate.{SLong, Values}
import tofu.syntax.raise._

import scala.util.Try

object dex {

  private val SellContractTokenPriceIndexInConstants = 5
  private val BuyContractTokenIdIndexInConstants     = 6
  private val BuyContractTokenAmountIndexInConstants = 8

  def sellContractInstance[F[_]: CRaise[*[_], DexContractInstantiationFailed]: Applicative](
    tokensPrice: Long
  ): F[ErgoTree] = {
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    Try {
      val anyPk = sigmastate.eval.SigmaDsl.SigmaProp(ProveDlog(constants.group.generator))
      AssetsAtomicExchangeCompilation.sellerContractInstance(tokensPrice, anyPk).ergoTree
    }.toEither
      .leftMap(e => DexContractInstantiationFailed(e.getMessage))
      .toRaise
  }

  def sellContractTemplate[F[_]: CRaise[*[_], DexErr]: CRaise[*[_], RefinementFailed]: Monad]
    : F[HexString] =
    // parameter values does not matter, we're extracting ErgoTree template (with placeholders in places of values)
    sellContractInstance[F](0L)
      .flatMap(ergoTreeTemplateBytes[F])
      .flatMap(bytes => HexString.fromString(Base16.encode(bytes)))

  def buyContractInstance[
    F[_]: CRaise[*[_], DexContractInstantiationFailed]: CRaise[*[_], Base16DecodingFailed]: Monad
  ](
    tokenId: TokenId,
    tokenAmount: Long
  ): F[ErgoTree] =
    utils.hexStringToBytes[F](tokenId.value).flatMap { tokenIdBytes =>
      // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
      Try {
        val anyPk =
          sigmastate.eval.SigmaDsl.SigmaProp(ProveDlog(constants.group.generator))
        AssetsAtomicExchangeCompilation
          .buyerContractInstance(
            tokenIdBytes.toColl,
            tokenAmount,
            anyPk
          )
          .ergoTree
      }.toEither
        .leftMap(e => DexContractInstantiationFailed(e.getMessage))
        .toRaise
    }

  def buyContractTemplate[
    F[_]: CRaise[*[_], DexErr]: CRaise[*[_], RefinementFailed]: Monad
  ]: F[HexString] =
    // parameter values does not matter, we're extracting ErgoTree template (with placeholders in places of values)
    TokenId
      .fromString[F]("21f84cf457802e66fb5930fb5d45fbe955933dc16a72089bf8980797f24e2fa1")
      .flatMap(buyContractInstance[F](_, tokenAmount = 0L))
      .flatMap(ergoTreeTemplateBytes[F])
      .flatMap(bytes => HexString.fromString(Base16.encode(bytes)))

  /** Extracts tokens price embedded in the DEX sell order contract
    * @param tree ErgoTree of the contract
    * @return tokens price
    */
  @inline def getTokenPriceFromSellContractTree[
    F[_]: CRaise[*[_], DexSellOrderAttributesFailed]: Applicative
  ](tree: ErgoTree): F[Long] =
    tree.constants
      .lift(SellContractTokenPriceIndexInConstants)
      .collect {
        case Values.ConstantNode(value, SLong) =>
          value.asInstanceOf[Long]
      }
      .orRaise(
        DexSellOrderAttributesFailed(
          s"Cannot find token price in constants($SellContractTokenPriceIndexInConstants) in sell order ergo tree $tree"
        )
      )

  /** Extracts tokens price embedded in the DEX sell order contract
    * @param ergoTreeStr Base16-encoded serialized ErgoTree of the contract
    * @return tokens price
    */
  def getTokenPriceFromSellOrderTree[
    F[_]: CRaise[*[_], DexErr]: FlatMap: Applicative
  ](ergoTreeStr: HexString): F[Long] =
    utils
      .hexStringToBytes[F](ergoTreeStr)
      .flatMap(bytes =>
        bytesToErgoTree[F](bytes)
          .flatMap(getTokenPriceFromSellContractTree[F])
      )

  /** Extracts token id and token amount embedded in the DEX buy order contract
    * @param tree ErgoTree of the contract
    * @return token id and token amount
    */
  @inline def getTokenInfoFromBuyContractTree[
    F[_]: CRaise[*[_], DexBuyOrderAttributesFailed]: CRaise[*[_], RefinementFailed]: Monad
  ](tree: ErgoTree): F[(TokenId, Long)] =
    tree.constants
      .lift(BuyContractTokenIdIndexInConstants)
      .collect {
        case ByteArrayConstant(coll) =>
          TokenId.fromString(Base16.encode(coll.toArray))
      }
      .orRaise(
        DexBuyOrderAttributesFailed(
          s"Cannot find tokenId in the buy order ergo tree $tree"
        )
      )
      .flatten
      .flatMap(tokenId =>
        tree.constants
          .lift(BuyContractTokenAmountIndexInConstants)
          .collect {
            case Values.ConstantNode(value, SLong) =>
              value.asInstanceOf[Long]
          }
          .map((tokenId, _))
          .orRaise(
            DexBuyOrderAttributesFailed(
              s"Cannot find token amount in the buy order ergo tree $tree"
            )
          )
      )

  /** Extracts token id and token amount embedded in the DEX buy order contract
    * @param ergoTreeStr Base16-encoded ErgoTree of the contract
    * @return token id and token amount
    */
  def getTokenInfoFromBuyOrderTree[
    F[_]: CRaise[*[_], DexErr]: CRaise[*[_], RefinementFailed]: Monad
  ](ergoTreeStr: HexString): F[(TokenId, Long)] =
    utils
      .hexStringToBytes[F](ergoTreeStr)
      .flatMap(bytes =>
        bytesToErgoTree[F](bytes)
          .flatMap(getTokenInfoFromBuyContractTree[F])
      )
}
