package org.ergoplatform.explorer.db

import eu.timepit.refined.refineMV
import eu.timepit.refined.string.HexStringSpec
import org.ergoplatform.explorer.Err.DexErr.{
  DexBuyOrderAttributesFailed,
  DexSellOrderAttributesFailed
}
import org.ergoplatform.explorer.{HexString, TokenId}
import scorex.util.encode.Base16
import sigmastate.{SLong, Values}
import sigmastate.Values.ByteArrayConstant
import sigmastate.serialization.ErgoTreeSerializer

import scala.util.{Failure, Success, Try}

object DexContracts {

  /** template for the buyer contract as of http://github.com/ScorexFoundation/sigmastate-interpreter/blob/42e55cbfd093252b8005e4607970764dd6610cbe/contract-verification/src/main/scala/sigmastate/verification/contract/AssetsAtomicExchange.scala#L33-L45
    */
  val sellContractTemplate: HexString = HexString(
    refineMV[HexStringSpec](
      "eb027300d1eded91b1a57301e6c6b2a5730200040ed801d60193e4c6b2a5730300040ec5a7eded92c1b2a57304007305720193c2b2a5730600d07307"
    )
  )

  /** template for the buyer contract as of http://github.com/ScorexFoundation/sigmastate-interpreter/blob/42e55cbfd093252b8005e4607970764dd6610cbe/contract-verification/src/main/scala/sigmastate/verification/contract/AssetsAtomicExchange.scala#L12-L32
    */
  val buyContractTemplate: HexString = HexString(
    refineMV[HexStringSpec](
      "eb027300d1eded91b1a57301e6c6b2a5730200040ed803d601e4c6b2a5730300020c4d0ed602eded91b172017304938cb27201730500017306928cb27201730700027308d60393e4c6b2a5730900040ec5a7eded720293c2b2a5730a00d0730b7203"
    )
  )

  private val treeSerializer: ErgoTreeSerializer = new ErgoTreeSerializer

  def getTokenPriceFromSellOrderTree(ergoTree: HexString): Try[Long] =
    Base16
      .decode(ergoTree.unwrapped)
      .flatMap { bytes =>
        val tree = treeSerializer.deserializeErgoTree(bytes)
        tree.constants
        // assuming buyer contract from http://github.com/ScorexFoundation/sigmastate-interpreter/blob/42e55cbfd093252b8005e4607970764dd6610cbe/contract-verification/src/main/scala/sigmastate/verification/contract/AssetsAtomicExchange.scala#L33-L45
          .lift(5)
          .collect {
            case Values.ConstantNode(value, SLong) =>
              Success(value.asInstanceOf[Long])
          }
          .getOrElse(
            Failure(
              DexSellOrderAttributesFailed(
                s"Cannot find tokenPrice in sell order ergo tree $ergoTree"
              )
            )
          )
      }

  case class TokenInfo(tokenId: TokenId, amount: Long)

  def getTokenInfoFromBuyOrderTree(ergoTree: HexString): Try[TokenInfo] =
    for {
      bytes <- Base16.decode(ergoTree.unwrapped)
      tree = treeSerializer.deserializeErgoTree(bytes)
      tokenId <- tree.constants
                // assuming seller contract from http://github.com/ScorexFoundation/sigmastate-interpreter/blob/42e55cbfd093252b8005e4607970764dd6610cbe/contract-verification/src/main/scala/sigmastate/verification/contract/AssetsAtomicExchange.scala#L33-L45
                  .lift(6)
                  .collect {
                    case ByteArrayConstant(coll) =>
                      Success(TokenId(Base16.encode(coll.toArray)))
                  }
                  .getOrElse(
                    Failure(
                      DexBuyOrderAttributesFailed(
                        s"Cannot find tokenId in the buy order ergo tree $ergoTree"
                      )
                    )
                  )
      tokenAmount <- tree.constants
                      .lift(8)
                      .collect {
                        case Values.ConstantNode(value, SLong) =>
                          Success(value.asInstanceOf[Long])
                      }
                      .getOrElse(
                        Failure(
                          DexBuyOrderAttributesFailed(
                            s"Cannot find token amount in the buy order ergo tree $ergoTree"
                          )
                        )
                      )

    } yield TokenInfo(tokenId, tokenAmount)
}
