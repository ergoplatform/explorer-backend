package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import org.ergoplatform.explorer.persistence.{RealDbTest, repositories}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class BlockExtensionRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert/getByHeaderId") {
    withLiveRepos { (headerRepo, repo) =>
      forSingleInstance(blockExtensionWithHeaderGen) {
        case (header, extension) =>
          headerRepo.insert(header).unsafeRunSync()
          repo.getByHeaderId(extension.headerId).unsafeRunSync() shouldBe None
          repo.insert(extension).unsafeRunSync()
          repo.getByHeaderId(extension.headerId).unsafeRunSync() shouldBe Some(extension)
      }
    }
  }

  private def withLiveRepos(
    body: (HeaderRepo.Live[IO], BlockExtensionRepo.Live[IO]) => Any
  ): Any = {
    val repo = new repositories.BlockExtensionRepo.Live[IO](xa)
    val headerRepo = new repositories.HeaderRepo.Live[IO](xa)
    body(headerRepo, repo)
  }
}
