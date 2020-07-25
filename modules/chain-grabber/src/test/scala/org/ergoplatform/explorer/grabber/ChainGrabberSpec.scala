package org.ergoplatform.explorer.grabber

import cats.effect._
import doobie.free.connection.ConnectionIO
import monocle.macros.syntax.lens._
import org.ergoplatform.explorer.MainNetConfiguration
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories.HeaderRepo
import org.ergoplatform.explorer.db.{RealDbTest, repositories}
import org.ergoplatform.explorer.grabber.GrabberTestNetworkClient.Source
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiTransaction}
import org.ergoplatform.explorer.settings.GrabberAppSettings
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.scalacheck.ScalacheckShapeless._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.concurrent.duration._

class ChainGrabberSpec
  extends PropSpec
  with ScalaCheckDrivenPropertyChecks
  with RealDbTest
  with MainNetConfiguration
  with Matchers {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.testConstants._

  private lazy val settings =
    GrabberAppSettings(1.second, mainnetNodes, dbSettings, protocolSettings)

  property("Network scanning") {
    forSingleInstance(consistentChainGen(12)) { apiBlocks =>
      withLiveRepo[ConnectionIO] { repo =>
        val networkService = new GrabberTestNetworkClient[IO](Source(apiBlocks))
        ChainGrabber[IO, ConnectionIO](settings, networkService)(xa.trans)
          .flatMap(_.run.take(11L).compile.drain)
          .unsafeRunSync()
        repo.getBestHeight.runWithIO() shouldBe 11
      }
    }
  }

  private def withLiveRepo[D[_]: LiftConnectionIO: Sync](
    body: HeaderRepo[D] => Any
  ): Any =
    body(repositories.HeaderRepo[IO, D].unsafeRunSync())

  private def consistentChainGen(length: Int): Gen[List[ApiFullBlock]] =
    for {
      blocks <- Gen.listOfN(length, implicitly[Arbitrary[ApiFullBlock]].arbitrary)
      txs    <- Gen.listOfN(length * 5, implicitly[Arbitrary[ApiTransaction]].arbitrary)
      blocksWithTxs = txs.grouped(5).toList.zip(blocks).map {
                        case (txs, b) => b.lens(_.transactions.transactions).modify(_ ++ txs)
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
      .map {
        case (block, idx) =>
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
