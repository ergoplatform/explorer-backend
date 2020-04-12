package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{RealDbTest, repositories}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class HeaderRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/get") {
    withHeaderRepo[ConnectionIO] { repo =>
      forSingleInstance(headerGen) { header =>
        repo.get(header.id).runWithIO() shouldBe None
        repo.insert(header).runWithIO()
        repo.get(header.id).runWithIO() shouldBe Some(header)
      }
    }
  }

  property("getAllByHeight") {
    withHeaderRepo[ConnectionIO] { repo =>
      forSingleInstance(headerGen) { header =>
        repo.insert(header).runWithIO()
        repo.getAllByHeight(header.height).runWithIO() shouldBe List(header)
      }
    }
  }

  property("getHeightOf") {
    withHeaderRepo[ConnectionIO] { repo =>
      forSingleInstance(headerGen) { header =>
        repo.insert(header).runWithIO()
        repo.getHeightOf(header.id).runWithIO() shouldBe Some(header.height)
      }
    }
  }

  private def withHeaderRepo[D[_]: LiftConnectionIO: Sync](
    body: HeaderRepo[D] => Any
  ): Any =
    body(repositories.HeaderRepo[IO, D].unsafeRunSync())
}
