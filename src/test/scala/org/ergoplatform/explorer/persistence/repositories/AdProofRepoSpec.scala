package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import org.ergoplatform.explorer.persistence.{repositories, RealDbTest}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AdProofRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert/getByHeaderId") {
    withLiveRepos { repo =>
      forAll(adProofGen) { proof =>
        repo.getByHeaderId(proof.headerId).unsafeRunSync() shouldBe None
        repo.insert(proof).unsafeRunSync()
        repo.getByHeaderId(proof.headerId).unsafeRunSync() shouldBe Some(proof)
      }
    }
  }

  private def withLiveRepos(
    body: AdProofRepo.Live[IO] => Any
  ): Any = {
    val repo = new repositories.AdProofRepo.Live[IO](xa)
    body(repo)
  }
}
