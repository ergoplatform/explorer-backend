package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}

import org.scalatest._
import flatspec._
import matchers._

class TransactionRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "TransactionRepo" should "insert/getMain" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) { case (header, txs) =>
        headerRepo.insert(header).runWithIO()
        txs.foreach { tx =>
          txRepo.getMain(tx.id).runWithIO() should be(None)
          txRepo.insert(tx).runWithIO()
          txRepo.getMain(tx.id).runWithIO() should be(Some(tx))
        }
      }
    }
  }

  it should "getMain (ignore transactions from forks)" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = false)) { case (header, txs) =>
        headerRepo.insert(header).runWithIO()
        txs.foreach { tx =>
          txRepo.insert(tx).runWithIO()
          txRepo.getMain(tx.id).runWithIO() should be(None)
        }
      }
    }
  }

  it should "getAllMainByIdSubstring" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) { case (header, txs) =>
        headerRepo.insert(header).runWithIO()
        txs.foreach { tx =>
          txRepo.insert(tx).runWithIO()
          txRepo
            .getAllMainByIdSubstring(tx.id.value.take(8))
            .runWithIO() should be(List(tx))
        }
      }
    }
  }

  it should "getAllByBlockId" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = true)) { case (header, txs) =>
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

  it should "updateChainStatusByHeaderId" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo) =>
      forSingleInstance(headerWithTxsGen(mainChain = false)) { case (header, txs) =>
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
