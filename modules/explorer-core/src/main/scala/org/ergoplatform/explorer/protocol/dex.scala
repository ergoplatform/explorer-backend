package org.ergoplatform.explorer.protocol

import cats.{Applicative, FlatMap, Monad}
import cats.syntax.flatMap._
import cats.instances.try_._
import org.ergoplatform.contracts.AssetsAtomicExchangeCompilation
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.{
  DexBuyOrderAttributesFailed,
  DexSellOrderAttributesFailed
}
import org.ergoplatform.explorer.Err.RequestProcessingErr.ContractParsingErr
import org.ergoplatform.explorer.protocol.utils.{
  bytesToErgoTree,
  ergoTreeTemplateBytes,
  hexStringBase16ToBytes
}
import org.ergoplatform.explorer.{HexString, TokenId}
import scorex.util.encode.Base16
import sigmastate.Values.{ByteArrayConstant, ErgoTree}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.Extensions._
import sigmastate.{SLong, Values}
import tofu.Raise.ContravariantRaise
import tofu.syntax.raise._

import scala.util.Try

object dex {

  private val SellContractTokenPriceIndexInConstants = 5
  private val BuyContractTokenIdIndexInConstants     = 6
  private val BuyContractTokenAmountIndexInConstants = 8

  def sellContractInstance(tokensPrice: Long): ErgoTree = {
    import org.ergoplatform.sigma.verified.VerifiedTypeConverters._
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk = sigmastate.eval.SigmaDsl.SigmaProp(ProveDlog(constants.group.generator))
    AssetsAtomicExchangeCompilation.sellerContractInstance(tokensPrice, anyPk).ergoTree
  }

  val sellContractTemplate: HexString = {
    // parameter values does not matter, we're extracting ErgoTree template (with placeholders in places of values)
    val contractErgoTree = sellContractInstance(0L)
    HexString.fromString[Try](Base16.encode(ergoTreeTemplateBytes(contractErgoTree))).get
  }

  def buyContractInstance(tokenId: TokenId, tokenAmount: Long): ErgoTree = {
    import org.ergoplatform.sigma.verified.VerifiedTypeConverters._
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk = sigmastate.eval.SigmaDsl.SigmaProp(ProveDlog(constants.group.generator))
    AssetsAtomicExchangeCompilation
      .buyerContractInstance(Base16.decode(tokenId.value).get.toColl, tokenAmount, anyPk)
      .ergoTree
  }

  val buyContractTemplate: HexString = {
    val anyToken = TokenId(
      "21f84cf457802e66fb5930fb5d45fbe955933dc16a72089bf8980797f24e2fa1"
    )
    // parameter values does not matter, we're extracting ErgoTree template (with placeholders in places of values)
    val contractErgoTree = buyContractInstance(anyToken, tokenAmount = 0L)
    HexString.fromString[Try](Base16.encode(ergoTreeTemplateBytes(contractErgoTree))).get
  }

  /** Extracts tokens price embedded in the DEX sell order contract
    * @param tree ErgoTree of the contract
    * @return tokens price
    */
  @inline def getTokenPriceFromSellContractTree[
    F[_]: ContravariantRaise[*[_], DexSellOrderAttributesFailed]: Applicative
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
    F[_]: ContravariantRaise[*[_], DexSellOrderAttributesFailed]: ContravariantRaise[*[_], ContractParsingErr]: FlatMap: Applicative
  ](ergoTreeStr: HexString): F[Long] =
    hexStringBase16ToBytes[F](ergoTreeStr)
      .flatMap(bytes =>
        bytesToErgoTree[F](bytes)
          .flatMap(getTokenPriceFromSellContractTree[F])
      )

  /** Extracts token id and token amount embedded in the DEX buy order contract
    * @param tree ErgoTree of the contract
    * @return token id and token amount
    */
  @inline def getTokenInfoFromBuyContractTree[
    F[_]: ContravariantRaise[*[_], DexBuyOrderAttributesFailed]: ContravariantRaise[*[_], RefinementFailed]: Monad
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
    F[_]: ContravariantRaise[*[_], DexBuyOrderAttributesFailed]: ContravariantRaise[*[_], ContractParsingErr]: ContravariantRaise[
      *[_],
      RefinementFailed
    ]: Monad
  ](ergoTreeStr: HexString): F[(TokenId, Long)] =
    hexStringBase16ToBytes[F](ergoTreeStr)
      .flatMap(bytes =>
        bytesToErgoTree[F](bytes)
          .flatMap(getTokenInfoFromBuyContractTree[F])
      )
}
