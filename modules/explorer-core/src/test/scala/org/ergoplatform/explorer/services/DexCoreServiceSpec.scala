package org.ergoplatform.explorer.services

import cats.effect.IO
import org.ergoplatform.explorer.db.DexContracts
import org.ergoplatform.explorer.db.models.aggregates.{DexSellOrderOutput, ExtendedOutput}
import org.ergoplatform.explorer.db.repositories.TestOutputRepo
import org.ergoplatform.explorer.db.repositories.TestOutputRepo.Source
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DexCoreServiceSpec
  extends PropSpec
  with Matchers
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.db.models.generators._

  property("Sell orders (enrich ExtendedOutput with token price)") {
    forAll(dexSellOrderGen) { sellOrderAssetPair =>
      val sellOrderExtOut = ExtendedOutput(sellOrderAssetPair._1, None)
      val source          = Source(List(sellOrderExtOut), List.empty)
      val outRepo         = new TestOutputRepo[IO](source)

      val expectedTokenPrice =
        DexContracts
          .getTokenPriceFromSellOrderTree[IO](sellOrderExtOut.output.ergoTree)
          .unsafeRunSync()

      val dexCoreService = DexCoreService[IO](outRepo)
      val expectedDexSellOrder =
        DexSellOrderOutput(sellOrderExtOut, expectedTokenPrice)
      val res = dexCoreService
        .getAllMainUnspentSellOrderByTokenId(sellOrderAssetPair._2.tokenId)
        .compile
        .toList
        .unsafeRunSync()
      res should contain theSameElementsAs (List(expectedDexSellOrder))
    }
  }

}
