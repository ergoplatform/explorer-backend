package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class BlockedTokenRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {
  import BlockedTokenRepoSpec._
  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "BlockedTokenRepo" should "insert & get blocked token" in {
    withBlockedTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(blockedTokenGen) { blockedToken =>
        repo.get(blockedToken.tokenId).runWithIO() should be(None)
        repo.insert(blockedToken).runWithIO()
        repo.get(blockedToken.tokenId).runWithIO() should be(Some(blockedToken))
      }
    }
  }
}

object BlockedTokenRepoSpec {

  private def withBlockedTokenRepo[D[_]: LiftConnectionIO: Sync](
    body: BlockedTokenRepo[D] => Any
  ): Any =
    body(repositories.BlockedTokenRepo[IO, D].unsafeRunSync())
}
