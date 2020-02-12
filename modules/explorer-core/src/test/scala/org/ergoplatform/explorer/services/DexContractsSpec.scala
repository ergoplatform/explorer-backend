package org.ergoplatform.explorer.services

import cats.effect.IO
import cats.instances.try_._
import org.ergoplatform.explorer.commonGenerators.assetIdGen
import org.ergoplatform.explorer.protocol.dex
import org.ergoplatform.explorer.protocol.dex.{
  getTokenInfoFromBuyContractTree,
  getTokenPriceFromSellContractTree
}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

class DexContractsSpec
  extends PropSpec
  with Matchers
  with ScalaCheckDrivenPropertyChecks {

  property("getTokenPriceFromSellOrderTree") {
    forAll(Gen.posNum[Long]) { tokenPrice =>
      val extractedTokenPrice =
        getTokenPriceFromSellContractTree[IO](
          dex.sellContractInstance(tokenPrice)
        ).unsafeRunSync()

      extractedTokenPrice shouldBe tokenPrice
    }
  }

  property("Buy orders (enrich ExtendedOutput with token info)") {
    forAll(assetIdGen, Gen.posNum[Long]) {
      case (tokenId, tokenAmount) =>
        val extractedTokenInfo =
          getTokenInfoFromBuyContractTree[IO](
            dex.buyContractInstance(tokenId, tokenAmount)
          ).unsafeRunSync()

    val expectedTokenInfo = (
      TokenId.fromString[Try]("21f84cf457802e66fb5930fb5d45fbe955933dc16a72089bf8980797f24e2fa1").get,
      60
    )
    extractedTokenInfo shouldEqual expectedTokenInfo
  }
}
