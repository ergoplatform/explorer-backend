package org.ergoplatform.explorer.indexer

import cats.effect._
import doobie.free.connection.ConnectionIO
import monocle.macros.syntax.lens._
import org.ergoplatform.explorer.MainNetConfiguration
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.HeaderRepo
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.indexer.GrabberTestNetwork.Source
import org.ergoplatform.explorer.indexer.processes.ChainIndexer
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiTransaction}
import org.ergoplatform.explorer.settings.{IndexerSettings, NetworkSettings}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.scalacheck.ScalacheckShapeless._
import org.scalacheck.{Arbitrary, Gen}
import tofu.concurrent.MakeRef
import tofu.logging.Logs

import org.scalatest._
import flatspec._
import matchers._

import scala.concurrent.duration._

@Ignore
class ChainGrabberSpec extends AnyFlatSpec with RealDbTest with MainNetConfiguration with should.Matchers {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.testConstants._

  private lazy val settings =
    IndexerSettings(
      pollInterval = 1.second,
      writeOrphans = true,
      network      = NetworkSettings(mainnetNodes, selfCheckIntervalRequests = 5),
      db           = dbSettings,
      protocol     = protocolSettings
    )

  implicit val logs: Logs[IO, IO]       = Logs.sync[IO, IO]
  implicit val makeRef: MakeRef[IO, IO] = MakeRef.syncInstance

  "ChainGrabber" should "perform Network scanning" in { // TODO: IGNORED TEST 1
    forSingleInstance(consistentChainGen(12)) { apiBlocks =>
      withLiveRepo[ConnectionIO] { repo =>
        val networkService = new GrabberTestNetwork[IO](Source(apiBlocks))
        ChainIndexer[IO, ConnectionIO](settings, networkService)(Trans.fromDoobie(xa))
          .flatMap(_.run.take(11L).compile.drain)
          .unsafeRunSync()
        repo.getBestHeight.runWithIO() should be(11)
      }
    }
  }

  private def withLiveRepo[D[_]: LiftConnectionIO: Sync](
    body: HeaderRepo[D, fs2.Stream] => Any
  ): Any =
    body(repositories.HeaderRepo[IO, D].unsafeRunSync())

  private def consistentChainGen(length: Int): Gen[List[ApiFullBlock]] =
    for {
      blocks <- Gen.listOfN(length, implicitly[Arbitrary[ApiFullBlock]].arbitrary)
      txs    <- Gen.listOfN(length * 5, implicitly[Arbitrary[ApiTransaction]].arbitrary)
      blocksWithTxs = txs.grouped(5).toList.zip(blocks).map { case (txs, b) =>
                        b.lens(_.transactions.transactions).modify(_ ++ txs)
                      }
    } yield linkRawBlocks(blocksWithTxs)

  private def linkRawBlocks(blocks: List[ApiFullBlock]) =
    blocks
      .foldLeft(List.empty[ApiFullBlock]) {
        case (Nil, block) =>
          List(block)
        case (acc @ parent :: _, block) =>
          val blockUpd = block
            .lens(_.header.parentId)
            .modify(_ => parent.header.id)
          blockUpd +: acc
      }
      .reverse
      .zipWithIndex
      .map { case (block, idx) =>
        val blockId = block.header.id
        block
          .lens(_.header.height)
          .modify(_ => idx)
          .lens(_.header.minerPk)
          .modify(_ => MainNetMinerPk)
          .lens(_.extension.headerId)
          .modify(_ => blockId)
          .lens(_.transactions.headerId)
          .modify(_ => blockId)
          .lens(_.adProofs)
          .modify(_.map(_.copy(headerId = blockId)))
      }
}
