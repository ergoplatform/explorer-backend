package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.ConnectionIO
import org.ergoplatform.explorer.{db, BoxId}
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.syntax.runConnectionIO._
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
    withLiveRepos[ConnectionIO] { (assetRepo, outputRepo, inputRepo, dexOrdersRepo) =>
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
            dexOrdersRepo
              .getAllMainUnspentSellOrderByTokenId(asset.tokenId)
              .compile
              .toList
              .runWithIO()
              .head shouldEqual ExtendedOutput(out, None)
        }

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
