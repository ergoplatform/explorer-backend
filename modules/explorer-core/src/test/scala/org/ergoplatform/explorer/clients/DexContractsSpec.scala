package org.ergoplatform.explorer.clients

import cats.effect.IO
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.commonGenerators.assetIdGen
import org.ergoplatform.explorer.protocol.constants
import org.ergoplatform.explorer.protocol.dex.{getTokenInfoFromBuyContractTree, getTokenPriceFromSellContractTree}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog

class DexContractsSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  property("getTokenPriceFromSellOrderTree") {
    forAll(tokenAmountGen) { tokenPrice =>
      val extractedTokenPrice =
        getTokenPriceFromSellContractTree[IO](sellContractInstance(tokenPrice))
          .unsafeRunSync()

      extractedTokenPrice shouldBe tokenPrice
    }
  }

  property("Buy orders (enrich ExtendedOutput with token info)") {
    forAll(assetIdGen, tokenAmountGen) { case (tokenId, tokenAmount) =>
      val extractedTokenInfo =
        getTokenInfoFromBuyContractTree[IO](buyContractInstance(tokenId, tokenAmount))
          .unsafeRunSync()

      val expectedTokenInfo = (tokenId, tokenAmount)
      extractedTokenInfo shouldEqual expectedTokenInfo
    }
  }

  def sellContractInstance(price: Long): ErgoTree = {
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk      = ProveDlog(constants.group.generator)
    val anyTokenId = Base16.decode("7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce").get
    val params     = DexSellerContractParameters(anyPk, anyTokenId, price, 0L)
    DexLimitOrderContracts.sellerContractInstance(params).ergoTree
  }

  def buyContractInstance(tokenId: TokenId, price: Long): ErgoTree = {
    // since we're only using compiled contracts for constant(parameters) extraction PK value does not matter
    val anyPk         = ProveDlog(constants.group.generator)
    val tokenIdNative = Digest32 @@ Base16.decode(tokenId.value.unwrapped).get
    val params        = DexBuyerContractParameters(anyPk, tokenIdNative, price, 0L)
    DexLimitOrderContracts.buyerContractInstance(params).ergoTree
  }

  private def tokenAmountGen = Gen.posNum[Long].map(_ + 2)
}
