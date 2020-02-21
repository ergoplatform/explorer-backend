package org.ergoplatform.explorer.services

import cats.effect.IO
import org.ergoplatform.explorer.commonGenerators.assetIdGen
import org.ergoplatform.explorer.protocol.dex
import org.ergoplatform.explorer.protocol.dex.{
  getTokenInfoFromBuyContractTree,
  getTokenPriceFromSellContractTree
}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DexContractsSpec
  extends PropSpec
  with Matchers
  with ScalaCheckDrivenPropertyChecks {

  property("getTokenPriceFromSellOrderTree") {
    forAll(Gen.posNum[Long]) { tokenPrice =>
      val extractedTokenPrice =
        dex
          .sellContractInstance[IO](tokenPrice)
          .flatMap(getTokenPriceFromSellContractTree[IO])
          .unsafeRunSync()

      extractedTokenPrice shouldBe tokenPrice
    }
  }

  property("Buy orders (enrich ExtendedOutput with token info)") {
    forAll(assetIdGen, Gen.posNum[Long]) {
      case (tokenId, tokenAmount) =>
        val extractedTokenInfo =
          dex
            .buyContractInstance[IO](tokenId, tokenAmount)
            .flatMap(getTokenInfoFromBuyContractTree[IO])
            .unsafeRunSync()

        val expectedTokenInfo = (tokenId, tokenAmount)
        extractedTokenInfo shouldEqual expectedTokenInfo
    }
  }
}
