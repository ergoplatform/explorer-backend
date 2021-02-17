package org.ergoplatform.explorer.protocol

import cats.syntax.flatMap._
import cats.{Applicative, FlatMap, Monad}
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.explorer.Err.RefinementFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr
import org.ergoplatform.explorer.Err.RequestProcessingErr.DexErr.{DexBuyOrderAttributesFailed, DexSellOrderAttributesFailed}
import org.ergoplatform.explorer.protocol.sigma.bytesToErgoTree
import org.ergoplatform.explorer.{CRaise, ErgoTreeTemplateHash, HexString, TokenId}
import scorex.crypto.hash.Sha256
import scorex.util.encode.Base16
import sigmastate.Values.{ByteArrayConstant, ErgoTree}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.{SLong, Values}
import tofu.syntax.raise._

object dex {

  private val SellContractTokenPriceIndexInConstants = 9
  private val BuyContractTokenIdIndexInConstants     = 1
  private val BuyContractTokenAmountIndexInConstants = 8

  // TODO ScalaDoc
  def sellContractInstance: ErgoTree = {
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk      = ProveDlog(constants.group.generator)
    val anyTokenId = Base16.decode("7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce").get
    val params     = DexSellerContractParameters(anyPk, anyTokenId, 10L, 10L)
    DexLimitOrderContracts.sellerContractInstance(params).ergoTree
  }

  def buyContractInstance: ErgoTree = {
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk      = ProveDlog(constants.group.generator)
    val anyTokenId = Base16.decode("7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce").get
    val params     = DexBuyerContractParameters(anyPk, anyTokenId, 10L, 10L)
    DexLimitOrderContracts.buyerContractInstance(params).ergoTree
  }

  def sellContractTemplateHash: ErgoTreeTemplateHash =
    ErgoTreeTemplateHash.fromStringUnsafe(Base16.encode(Sha256.hash(sellContractInstance.template)))

  def buyContractTemplateHash: ErgoTreeTemplateHash =
    ErgoTreeTemplateHash.fromStringUnsafe(Base16.encode(Sha256.hash(buyContractInstance.template)))

  /** Extracts tokens price embedded in the DEX sell order contract
    * @param tree ErgoTree of the contract
    * @return tokens price
    */
  @inline def getTokenPriceFromSellContractTree[
    F[_]: CRaise[*[_], DexSellOrderAttributesFailed]: Applicative
  ](tree: ErgoTree): F[Long] =
    tree.constants
      .lift(SellContractTokenPriceIndexInConstants)
      .collect { case Values.ConstantNode(value, SLong) =>
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
    sigma
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
      .collect { case ByteArrayConstant(coll) =>
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
          .collect { case Values.ConstantNode(value, SLong) =>
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
    sigma
      .hexStringToBytes[F](ergoTreeStr)
      .flatMap(bytes =>
        bytesToErgoTree[F](bytes)
          .flatMap(getTokenInfoFromBuyContractTree[F])
      )
}
