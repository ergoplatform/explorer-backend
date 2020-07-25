package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class TransactionRepoSpec extends PropSpec with Matchers with RealDbTest with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getMain") {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) {
        case (header, txs) =>
          headerRepo.insert(header).runWithIO()
          txs.foreach { tx =>
            txRepo.getMain(tx.id).runWithIO() shouldBe None
            txRepo.insert(tx).runWithIO()
            txRepo.getMain(tx.id).runWithIO() shouldBe Some(tx)
          }
      }
    }
  }

  property("getMain (ignore transactions from forks)") {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = false)) {
        case (header, txs) =>
          headerRepo.insert(header).runWithIO()
          txs.foreach { tx =>
            txRepo.insert(tx).runWithIO()
            txRepo.getMain(tx.id).runWithIO() shouldBe None
          }
      }
    }
  }

  property("getAllMainByIdSubstring") {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) {
        case (header, txs) =>
          headerRepo.insert(header).runWithIO()
          txs.foreach { tx =>
            txRepo.insert(tx).runWithIO()
            txRepo
              .getAllMainByIdSubstring(tx.id.value.take(8))
              .runWithIO() shouldBe List(tx)
          }
      }
    }
  }

  property("getAllByBlockId") {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) {
        case (header, txs) =>
          headerRepo.insert(header).runWithIO()
          txs.foreach { tx =>
            txRepo.insert(tx).runWithIO()
          }
          txRepo
            .getAllByBlockId(header.id)
            .compile
            .toList
            .runWithIO() should contain theSameElementsAs txs
      }
    }
  }

  property("updateChainStatusByHeaderId") {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = false)) {
        case (header, txs) =>
          headerRepo.insert(header).runWithIO()
          txs.foreach { tx =>
            txRepo.insert(tx).runWithIO()
          }
          txRepo
            .updateChainStatusByHeaderId(header.id, newChainStatus = true)
            .runWithIO()
          txRepo.getAllByBlockId(header.id).compile.toList.runWithIO() should contain theSameElementsAs txs
            .map(_.copy(mainChain = true))
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (HeaderRepo[D], TransactionRepo[D, fs2.Stream]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.TransactionRepo[IO, D].unsafeRunSync()
    )
}
