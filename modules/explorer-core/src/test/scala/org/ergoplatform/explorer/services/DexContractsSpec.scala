package org.ergoplatform.explorer.services

import cats.effect.IO
import cats.instances.try_._
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.protocol.dex
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

class DexContractsSpec
  extends PropSpec
  with Matchers
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.db.models.generators._

  property("getTokenPriceFromSellOrderTree") {
    val extractedTokenPrice =
      dex
        .getTokenPriceFromSellOrderTree[IO](sellOrderErgoTree)
        .unsafeRunSync()

    extractedTokenPrice shouldBe 50000000L
  }

  property("Buy orders (enrich ExtendedOutput with token info)") {
    val extractedTokenInfo =
      dex.getTokenInfoFromBuyOrderTree[IO](buyOrderErgoTree).unsafeRunSync()

    val expectedTokenInfo = (
      TokenId.fromString[Try]("21f84cf457802e66fb5930fb5d45fbe955933dc16a72089bf8980797f24e2fa1").get,
      60
    )
    extractedTokenInfo shouldEqual expectedTokenInfo
  }
}
