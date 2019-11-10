package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import org.ergoplatform.explorer.persistence
import org.ergoplatform.explorer.persistence.RealDbTest
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssetRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert/getAllByBoxId") {
    withLiveRepo { repo =>
      forSingleInstance(assetsWithBoxIdGen) {
        case (boxId, assets) =>
          repo.getAllByBoxId(boxId).unsafeRunSync() shouldBe 'empty
          assets.foreach { asset =>
            repo.insert(asset).unsafeRunSync()
          }
          repo
            .getAllByBoxId(boxId)
            .unsafeRunSync() should contain theSameElementsAs assets
      }
    }
  }

  private def withLiveRepo(
    body: AssetRepo.Live[IO] => Any
  ): Any = {
    val assetRepo = new persistence.repositories.AssetRepo.Live[IO](xa)
    body(assetRepo)
  }
}
