package org.ergoplatform.explorer.db.repositories

import cats.effect.Sync
import doobie.ConnectionIO
import org.ergoplatform.explorer.db
import org.ergoplatform.explorer.db.RealDbTest
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.syntax.runConnectionIO._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssetRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getAllByBoxId") {
    withLiveRepo[ConnectionIO] { repo =>
      forSingleInstance(assetsWithBoxIdGen) {
        case (boxId, assets) =>
          repo.getAllByBoxId(boxId).runWithIO() shouldBe 'empty
          assets.foreach { asset =>
            repo.insert(asset).runWithIO()
          }
          repo
            .getAllByBoxId(boxId)
            .runWithIO() should contain theSameElementsAs assets
      }
    }
  }

  private def withLiveRepo[D[_]: LiftConnectionIO: Sync](
    body: AssetRepo[D, fs2.Stream[D, *]] => Any
  ): Any =
    body(db.repositories.AssetRepo[D])
}
