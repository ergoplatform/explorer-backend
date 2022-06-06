package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.ValidByte
import eu.timepit.refined.auto._
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.scalatest._
import flatspec._
import matchers._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.protocol.sigma

import scala.util.Try

class TransactionRepoSpec extends AnyFlatSpec with should.Matchers with TryValues with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "TransactionRepo" should "insert & get main Transaction" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo, _, _) =>
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
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo, _, _) =>
      forSingleInstance(headerWithTxsGen(mainChain = false)) { case (header, txs) =>
        headerRepo.insert(header).runWithIO()
        txs.foreach { tx =>
          txRepo.insert(tx).runWithIO()
          txRepo.getMain(tx.id).runWithIO() should be(None)
        }
      }
    }
  }

  it should "get all main transaction by id substring" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo, _, _) =>
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

  it should "get all transactions by block ID" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo, _, _) =>
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

  it should "update tx chain status by header ID" in {
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo, _, _) =>
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

  it should "count all txs between inclusion height range" in {
    val addressS                                    = "3WzSdM7NrjDJswpu2ThfhWvVM1mKJhgnGNieWYcGVsYp3AoirgR5"
    val networkPrefix: String Refined ValidByte     = "16"
    implicit val addressEncoder: ErgoAddressEncoder = ErgoAddressEncoder(networkPrefix.value.toByte)
    val addressT                                    = Address.fromString[Try](addressS)
    val addressTree                                 = sigma.addressToErgoTreeHex(addressT.get)
    addressT.isSuccess should be(true)
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo, outputRepo, inputRepo) =>
      forSingleInstance(`headerTxsOutputs&InputGen`(mainChain = true, 10, 20, addressT.get, addressTree)) { hTxList =>
        hTxList.foreach { case (header, tx, out, in) =>
          headerRepo.insert(header).runWithIO()
          txRepo.insert(tx).runWithIO()
          outputRepo.insert(out).runWithIO()
          inputRepo.insert(in).runWithIO()
        }

        txRepo.countRelatedToAddress(addressT.get).runWithIO() should be(11)
        txRepo.countRelatedToAddress(addressT.get, Some((10, 20))).runWithIO() should be(11)
        txRepo.countRelatedToAddress(addressT.get, Some((10, 15))).runWithIO() should be(6)
        txRepo.countRelatedToAddress(addressT.get, Some((15, 18))).runWithIO() should be(4)
      }
    }
  }

  it should "get all txs between inclusion height range" in {
    val addressS                                    = "3Wx99DApJTpUTPZDhYEerbqWfa9MvuuVJehAFVeepnZMzAN3dfYW"
    val networkPrefix: String Refined ValidByte     = "16"
    implicit val addressEncoder: ErgoAddressEncoder = ErgoAddressEncoder(networkPrefix.value.toByte)
    val addressT                                    = Address.fromString[Try](addressS)
    val addressTree                                 = sigma.addressToErgoTreeHex(addressT.get)
    addressT.isSuccess should be(true)
    withLiveRepos[ConnectionIO] { (headerRepo, txRepo, outputRepo, inputRepo) =>
      forSingleInstance(`headerTxsOutputs&InputGen`(mainChain = true, 10, 20, addressT.get, addressTree)) { hTxList =>
        hTxList.foreach { case (header, tx, out, in) =>
          headerRepo.insert(header).runWithIO()
          txRepo.insert(tx).runWithIO()
          outputRepo.insert(out).runWithIO()
          inputRepo.insert(in).runWithIO()
        }

        txRepo.countRelatedToAddress(addressT.get).runWithIO() should be(11)
        txRepo
          .streamRelatedToAddress(addressT.get, 0, Int.MaxValue, Some((10, 20)))
          .compile
          .toList
          .runWithIO() should contain theSameElementsAs hTxList.map(_._2)
        txRepo
          .streamRelatedToAddress(addressT.get, 0, Int.MaxValue, Some((12, 18)))
          .compile
          .toList
          .runWithIO() should contain theSameElementsAs hTxList
          .map(_._2)
          .filter(t => t.inclusionHeight >= 12 && t.inclusionHeight <= 18)
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (HeaderRepo[D, fs2.Stream], TransactionRepo[D, fs2.Stream], OutputRepo[D, fs2.Stream], InputRepo[D]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.TransactionRepo[IO, D].unsafeRunSync(),
      repositories.OutputRepo[IO, D].unsafeRunSync(),
      repositories.InputRepo[IO, D].unsafeRunSync()
    )
}
