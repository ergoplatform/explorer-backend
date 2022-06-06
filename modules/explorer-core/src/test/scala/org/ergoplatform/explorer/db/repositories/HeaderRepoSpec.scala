package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{repositories, RealDbTest}

import org.scalatest._
import flatspec._
import matchers._

class HeaderRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "HeaderRepo" should "insert/get" in {
    withHeaderRepo[ConnectionIO] { repo =>
      forSingleInstance(headerGen) { header =>
        repo.get(header.id).runWithIO() should be(None)
        repo.insert(header).runWithIO()
        repo.get(header.id).runWithIO() should be(Some(header))
      }
    }
  }

  it should "getAllByHeight" in {
    withHeaderRepo[ConnectionIO] { repo =>
      forSingleInstance(headerGen) { header =>
        repo.insert(header).runWithIO()
        repo.getAllByHeight(header.height).runWithIO() should be(List(header))
      }
    }
  }

  it should "getHeightOf" in {
    withHeaderRepo[ConnectionIO] { repo =>
      forSingleInstance(headerGen) { header =>
        repo.insert(header).runWithIO()
        repo.getHeightOf(header.id).runWithIO() should be(Some(header.height))
      }
    }
  }

  private def withHeaderRepo[D[_]: LiftConnectionIO: Sync](
    body: HeaderRepo[D, fs2.Stream] => Any
  ): Any =
    body(repositories.HeaderRepo[IO, D].unsafeRunSync())
}
