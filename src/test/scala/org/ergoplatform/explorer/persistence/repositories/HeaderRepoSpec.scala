package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import org.ergoplatform.explorer.persistence.{RealDbTest, repositories}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class HeaderRepoSpec
  extends PropSpec
    with Matchers
    with RealDbTest
    with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert") {
    withHeaderRepoLive { repo =>
      withSingleInstance(headerGen) { header =>
        repo.insert(header).unsafeRunSync()
        repo.get(header.id).unsafeRunSync() shouldBe Some(header)
      }
    }
  }

  private def withHeaderRepoLive(body: HeaderRepo[IO] => Any): Any = {
    val repo = new repositories.HeaderRepo.Live[IO](xa)
    body(repo)
  }
}
