package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.GenuineToken
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class GenuineTokenRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {
  import GenuineTokenRepoSpec._
  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "GenuineTokenRepo" should "insert & get genuine token" in {
    withGenuineTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(genuineTokenGen("TOKEN#1A", uniqueName = true)) { genuineToken =>
        repo.get(genuineToken.id).runWithIO() should be(None)
        repo.insertUnsafe(genuineToken).runWithIO()
        repo.get(genuineToken.id).runWithIO() should be(Some(genuineToken))
      }
    }
  }

  it should "not insert a token with unique name set to true, if that token name already exists" in {
    withGenuineTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(
        genuineTokenListGen(
          List(
            ("TOKEN#1C", false),
            ("TOKEN#1C", true),
            ("TOKEN#1C", false),
            ("TOKEN#1C", false)
          )
        )
      ) { genuineTokens =>
        val tkF = genuineTokens.filter(_.uniqueName == false)
        val tkT = genuineTokens.filter(_.uniqueName == true).head

        genuineTokens.foreach(repo.insert(_).runWithIO())

        repo.get(tkT.id).runWithIO() should be(None)
        repo.getByNameAndUnique("TOKEN#1C", unique = false).runWithIO() should be(tkF)
        repo.getByNameAndUnique("TOKEN#1C", unique = true).runWithIO() should be(List())
      }
    }
  }

  it should "not insert a token with a tokenName if that tokenName exist with unique name set to true" in {
    withGenuineTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(
        genuineTokenListGen(
          List(
            ("TOKEN#1B", true),
            ("TOKEN#1B", false),
            ("TOKEN#1B", false),
            ("TOKEN#1B", false)
          )
        )
      ) { genuineTokens =>
        val tksT = genuineTokens.filter(_.uniqueName == true)
        val tkT  = genuineTokens.filter(_.uniqueName == true).head

        genuineTokens.foreach(repo.insert(_).runWithIO())

        repo.get(tkT.id).runWithIO() should be(Some(tkT))
        repo.getByNameAndUnique("TOKEN#1B", unique = true).runWithIO() should be(tksT)
        repo.getByNameAndUnique("TOKEN#1B", unique = false).runWithIO() should be(List())
      }
    }
  }

  it should "get unique genuine token by name" in {
    withGenuineTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(genuineTokenGen("TOKEN#1D", uniqueName = true)) { genuineToken =>
        repo.insertUnsafe(genuineToken).runWithIO()
        repo.getByNameAndUnique("TOKEN#1D", unique = true).runWithIO() should be(List(genuineToken))
      }
    }
  }

  it should "get genuine tokens by name and uniqueness" in {
    withGenuineTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(
        genuineTokenListGen(
          List(
            ("TOKEN#1E", false),
            ("TOKEN#1E", false),
            ("TOKEN#1E", false),
            ("TOKEN#2E", true)
          )
        )
      ) { genuineTokens =>
        val tk1 = genuineTokens.filter(_.tokenName == "TOKEN#1E")
        val tk2 = genuineTokens.filter(_.tokenName == "TOKEN#2E")
        genuineTokens.foreach(repo.insertUnsafe(_).runWithIO())

        repo.getByNameAndUnique("TOKEN#1E", unique = true).runWithIO() should be(List.empty[GenuineToken])
        repo.getByNameAndUnique("TOKEN#1E", unique = false).runWithIO() should be(tk1)
        repo.getByNameAndUnique("TOKEN#2E", unique = true).runWithIO() should be(tk2)
      }
    }
  }

}

object GenuineTokenRepoSpec {

  private def withGenuineTokenRepo[D[_]: LiftConnectionIO: Sync](
    body: GenuineTokenRepo[D] => Any
  ): Any =
    body(repositories.GenuineTokenRepo[IO, D].unsafeRunSync())
}
