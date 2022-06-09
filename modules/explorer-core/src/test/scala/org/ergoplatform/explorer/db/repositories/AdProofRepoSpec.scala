package org.ergoplatform.explorer.db.repositories

import cats.Functor
import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{repositories, RealDbTest}

import org.scalatest._
import flatspec._
import matchers._

class AdProofRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "AdProofRepo" should "insert AdProof & get AdProof by HeaderId" in {
    withLiveRepos[ConnectionIO] { (headerRepo, repo) =>
      forSingleInstance(adProofWithHeaderGen) { case (header, proof) =>
        headerRepo.insert(header).runWithIO
        repo.getByHeaderId(proof.headerId).runWithIO should be(None)
        repo.insert(proof).runWithIO
        repo.getByHeaderId(proof.headerId).runWithIO should be(Some(proof))
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Functor](
    body: (HeaderRepo[D, fs2.Stream], AdProofRepo[D]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.AdProofRepo[IO, D].unsafeRunSync()
    )
}
