package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import org.ergoplatform.explorer.TxId
import org.ergoplatform.explorer.persistence.{RealDbTest, repositories}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class TransactionRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert/getMain") {
    withLiveRepos { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) {
        case (header, txs) =>
          headerRepo.insert(header).unsafeRunSync()
          txs.foreach { tx =>
            txRepo.getMain(tx.id).unsafeRunSync() shouldBe None
            txRepo.insert(tx).unsafeRunSync()
            txRepo.getMain(tx.id).unsafeRunSync() shouldBe Some(tx)
          }
      }
    }
  }

  property("getMain (ignore transactions from forks)") {
    withLiveRepos { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = false)) {
        case (header, txs) =>
          headerRepo.insert(header).unsafeRunSync()
          txs.foreach { tx =>
            txRepo.insert(tx).unsafeRunSync()
            txRepo.getMain(tx.id).unsafeRunSync() shouldBe None
          }
      }
    }
  }

  property("getAllMainByIdSubstring") {
    withLiveRepos { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) {
        case (header, txs) =>
          headerRepo.insert(header).unsafeRunSync()
          txs.foreach { tx =>
            txRepo.insert(tx).unsafeRunSync()
            txRepo.getAllMainByIdSubstring(TxId.unwrap(tx.id).take(8))
              .unsafeRunSync() shouldBe List(tx)
          }
      }
    }
  }

  property("getAllByBlockId") {
    withLiveRepos { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) {
        case (header, txs) =>
          headerRepo.insert(header).unsafeRunSync()
          txs.foreach { tx =>
            txRepo.insert(tx).unsafeRunSync()
          }
          txRepo.getAllByBlockId(header.id).compile.toList
            .unsafeRunSync() should contain theSameElementsAs txs
      }
    }
  }

  private def withLiveRepos(
    body: (HeaderRepo[IO], TransactionRepo.Live[IO]) => Any
  ): Any = {
    val headerRepo = new repositories.HeaderRepo.Live[IO](xa)
    val txRepo = new repositories.TransactionRepo.Live[IO](xa)
    body(headerRepo, txRepo)
  }
}
