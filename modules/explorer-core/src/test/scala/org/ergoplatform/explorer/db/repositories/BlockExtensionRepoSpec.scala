package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class BlockExtensionRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getByHeaderId") {
    withLiveRepos[ConnectionIO] { (headerRepo, repo) =>
      forSingleInstance(blockExtensionWithHeaderGen) {
        case (header, extension) =>
          headerRepo.insert(header).runWithIO()
          repo.getByHeaderId(extension.headerId).runWithIO() shouldBe None
          repo.insert(extension).runWithIO()
          repo.getByHeaderId(extension.headerId).runWithIO() shouldBe Some(extension)
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (HeaderRepo[D], BlockExtensionRepo[D]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.BlockExtensionRepo[IO, D].unsafeRunSync()
    )
}
