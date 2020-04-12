package org.ergoplatform.explorer.db.repositories

import cats.Functor
import cats.effect.{IO, Sync, SyncIO}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AdProofRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getByHeaderId") {
    withLiveRepos[ConnectionIO] { (headerRepo, repo) =>
      forSingleInstance(adProofWithHeaderGen) {
        case (header, proof) =>
          headerRepo.insert(header).runWithIO
          repo.getByHeaderId(proof.headerId).runWithIO shouldBe None
          repo.insert(proof).runWithIO
          repo.getByHeaderId(proof.headerId).runWithIO shouldBe Some(proof)
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Functor](
    body: (HeaderRepo[D], AdProofRepo[D]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.AdProofRepo[IO, D].unsafeRunSync()
    )
}
