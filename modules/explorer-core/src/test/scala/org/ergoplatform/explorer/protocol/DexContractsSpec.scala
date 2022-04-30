package org.ergoplatform.explorer.protocol

import cats.effect.IO
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.commonGenerators.assetIdGen
import org.ergoplatform.explorer.protocol.dex.{getTokenInfoFromBuyContractTree, getTokenPriceFromSellContractTree}
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.scalatest._
import flatspec._
import matchers._

class DexContractsSpec extends AnyFlatSpec with should.Matchers with ScalaCheckDrivenPropertyChecks {

  "DexContract/Protocol" should "Get token price from sell order tree" in { // getTokenPriceFromSellOrderTree
    forAll(Gen.posNum[Long]) { tokenPrice =>
      whenever(tokenPrice > 1) {
        val extractedTokenPrice =
          getTokenPriceFromSellContractTree[IO](sellContractInstance(tokenPrice))
            .unsafeRunSync()

        extractedTokenPrice should be(tokenPrice)
      }
    }
  }

  it should "Buy orders (enrich ExtendedOutput with token info)" in {
    forAll(assetIdGen, Gen.posNum[Long]) { case (tokenId, tokenAmount) =>
      whenever(tokenAmount > 1) {
        val extractedTokenInfo =
          getTokenInfoFromBuyContractTree[IO](buyContractInstance(tokenId, tokenAmount))
            .unsafeRunSync()

        val expectedTokenInfo = (tokenId, tokenAmount)
        extractedTokenInfo should equal(expectedTokenInfo)
      }
    }
  }

  def sellContractInstance(price: Long): ErgoTree = {
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk      = ProveDlog(constants.group.generator)
    val anyTokenId = Base16.decode("7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce").get
    val params     = DexSellerContractParameters(anyPk, anyTokenId, price, 100L)
    DexLimitOrderContracts.sellerContractInstance(params).ergoTree
  }

  def buyContractInstance(tokenId: TokenId, price: Long): ErgoTree = {
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk         = ProveDlog(constants.group.generator)
    val tokenIdNative = Digest32 @@ Base16.decode(tokenId.value.unwrapped).get
    val params        = DexBuyerContractParameters(anyPk, tokenIdNative, price, 100L)
    DexLimitOrderContracts.buyerContractInstance(params).ergoTree
  }
}
