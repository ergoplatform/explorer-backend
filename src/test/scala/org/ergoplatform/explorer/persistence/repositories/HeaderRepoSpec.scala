package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import org.ergoplatform.explorer.persistence.{repositories, RealDbTest}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class HeaderRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert/get") {
    withHeaderRepoLive { repo =>
      withSingleInstance(headerGen) { header =>
        repo.get(header.id).unsafeRunSync() shouldBe None
        repo.insert(header).unsafeRunSync()
        repo.get(header.id).unsafeRunSync() shouldBe Some(header)
      }
    }
  }

  property("update") {
    withHeaderRepoLive { repo =>
      withSingleInstance(headerGen) { header =>
        repo.insert(header).unsafeRunSync()
        val header2 = header.copy(height = header.height + 1)
        repo.update(header2).unsafeRunSync()
        repo.get(header.id).unsafeRunSync() shouldBe Some(header2)
      }
    }
  }

  property("getAllByHeight") {
    withHeaderRepoLive { repo =>
      withSingleInstance(headerGen) { header =>
        repo.insert(header).unsafeRunSync()
        repo.getAllByHeight(header.height).unsafeRunSync() shouldBe List(header)
      }
    }
  }

  property("getHeightOf") {
    withHeaderRepoLive { repo =>
      withSingleInstance(headerGen) { header =>
        repo.insert(header).unsafeRunSync()
        repo.getHeightOf(header.id).unsafeRunSync() shouldBe Some(header.height)
      }
    }
  }

  private def withHeaderRepoLive(body: HeaderRepo[IO] => Any): Any = {
    val repo = new repositories.HeaderRepo.Live[IO](xa)
    body(repo)
  }
}
