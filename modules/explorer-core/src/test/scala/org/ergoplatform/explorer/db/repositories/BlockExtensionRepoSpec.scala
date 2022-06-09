package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}

import org.scalatest._
import flatspec._
import matchers._

class BlockExtensionRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "BlockExtensionRepo" should "insert/getByHeaderId" in {
    withLiveRepos[ConnectionIO] { (headerRepo, repo) =>
      forSingleInstance(blockExtensionWithHeaderGen) { case (header, extension) =>
        headerRepo.insert(header).runWithIO()
        repo.getByHeaderId(extension.headerId).runWithIO() should be(None)
        repo.insert(extension).runWithIO()
        repo.getByHeaderId(extension.headerId).runWithIO() should be(Some(extension))
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (HeaderRepo[D, fs2.Stream], BlockExtensionRepo[D]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.BlockExtensionRepo[IO, D].unsafeRunSync()
    )
}
