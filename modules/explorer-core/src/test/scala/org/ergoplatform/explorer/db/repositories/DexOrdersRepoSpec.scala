package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.ConnectionIO
import org.ergoplatform.explorer.{db, BoxId, TokenId}
import org.ergoplatform.explorer.db.{repositories, DexContracts, RealDbTest}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.{
  DexBuyOrderOutput,
  DexSellOrderOutput,
  ExtendedOutput
}
import org.ergoplatform.explorer.db.syntax.runConnectionIO._
import org.http4s.Credentials.Token
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DexOrdersRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getUnspentSellOrders") {
    withLiveRepos[ConnectionIO] { (assetRepo, outputRepo, _, dexOrdersRepo) =>
      forSingleInstance(dexSellOrdersGen(5)) { sellOrders =>
        val arbTokenId = assetIdGen.retryUntil(_ => true).sample.get
        dexOrdersRepo
          .getAllMainUnspentSellOrderByTokenId(arbTokenId)
          .compile
          .toList
          .runWithIO() shouldBe empty

        sellOrders.foreach {
          case (out, asset) =>
            assetRepo.insert(asset).runWithIO()
            outputRepo.insert(out).runWithIO()
        }

        sellOrders.foreach {
          case (out, asset) =>
            val expectedSellOrder = DexSellOrderOutput(
              ExtendedOutput(out, None),
              DexContracts.getTokenPriceFromSellOrderTree(out.ergoTree).get
            )
            dexOrdersRepo
              .getAllMainUnspentSellOrderByTokenId(asset.tokenId)
              .compile
              .toList
              .runWithIO() should contain theSameElementsAs List(expectedSellOrder)
        }

        dexOrdersRepo
          .getAllMainUnspentSellOrderByTokenId(arbTokenId)
          .compile
          .toList
          .runWithIO() shouldBe empty
      }
    }
  }

  property("insert/getUnspentBuyOrders") {
    withLiveRepos[ConnectionIO] { (_, outputRepo, _, dexOrdersRepo) =>
      forSingleInstance(dexBuyOrderGen) { buyOrder =>
        val arbTokenId = assetIdGen.retryUntil(_ => true).sample.get
        dexOrdersRepo
          .getAllMainUnspentBuyOrderByTokenId(arbTokenId)
          .compile
          .toList
          .runWithIO() shouldBe empty

        outputRepo.insert(buyOrder).runWithIO()

        val tokenHardcodedInContract =
          TokenId("21f84cf457802e66fb5930fb5d45fbe955933dc16a72089bf8980797f24e2fa1")

        val expectedBuyOrder = DexBuyOrderOutput(
          ExtendedOutput(buyOrder, None),
          DexContracts.getTokenInfoFromBuyOrderTree(buyOrder.ergoTree).get
        )

        dexOrdersRepo
          .getAllMainUnspentBuyOrderByTokenId(tokenHardcodedInContract)
          .compile
          .toList
          .runWithIO() should contain theSameElementsAs List(expectedBuyOrder)

        dexOrdersRepo
          .getAllMainUnspentBuyOrderByTokenId(arbTokenId)
          .compile
          .toList
          .runWithIO() shouldBe empty
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      AssetRepo[D, fs2.Stream],
      OutputRepo[D, fs2.Stream],
      InputRepo[D],
      DexOrdersRepo[D, fs2.Stream]
    ) => Any
  ): Any =
    body(
      db.repositories.AssetRepo[D],
      db.repositories.OutputRepo[D],
      db.repositories.InputRepo[D],
      db.repositories.DexOrdersRepo[D]
    )

}
