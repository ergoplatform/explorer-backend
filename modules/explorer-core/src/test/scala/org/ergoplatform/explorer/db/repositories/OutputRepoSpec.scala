package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import cats.instances.try_._
import cats.syntax.option._
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.ergoplatform.explorer.protocol.dex
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

class OutputRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getByBoxId") {
    withLiveRepos[ConnectionIO] { (hRepo, txRepo, oRepo, _) =>
      forSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) {
        case (header, tx, outputs) =>
          hRepo.insert(header).runWithIO()
          txRepo.insert(tx).runWithIO()
          outputs.foreach { extOut =>
            oRepo.getByBoxId(extOut.output.boxId).runWithIO() shouldBe None
            oRepo.insert(extOut.output).runWithIO()
            oRepo.getByBoxId(extOut.output.boxId).runWithIO() shouldBe Some(extOut)
          }
      }
    }
  }

  property("getAllByAddress/getAllByErgoTree") {
    withLiveRepos[ConnectionIO] { (hRepo, txRepo, oRepo, _) =>
      forSingleInstance(hexStringRGen.flatMap(hex => addressGen.map(_ -> hex))) {
        case (address, ergoTree) =>
          forSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) {
            case (header, tx, outputs) =>
              hRepo.insert(header).runWithIO()
              txRepo.insert(tx).runWithIO()
              val nonMatching = outputs.head
              val matching = outputs.tail
                .map { extOut =>
                  extOut.copy(
                    output =
                      extOut.output.copy(addressOpt = address.some, ergoTree = ergoTree)
                  )
                }
              matching.foreach { extOut =>
                oRepo.insert(extOut.output).runWithIO()
              }
              oRepo.insert(nonMatching.output).runWithIO()
              oRepo
                .getByErgoTree(ergoTree, 0, Int.MaxValue)
                .compile
                .toList
                .runWithIO() should contain theSameElementsAs matching
          }
      }
    }
  }

  property("insert/getUnspentSellOrders") {
    withLiveRepos[ConnectionIO] { (_, _, outputRepo, assetRepo) =>
      forSingleInstance(dexSellOrdersGen(5)) { sellOrders =>
        val contractTemplate = dex.sellContractTemplate[IO].unsafeRunSync()
        val arbTokenId       = assetIdGen.retryUntil(_ => true).sample.get
        outputRepo
          .getAllMainUnspentSellOrderByTokenId(
            arbTokenId,
            contractTemplate,
            0,
            Int.MaxValue
          )
          .compile
          .toList
          .runWithIO() shouldBe empty

        sellOrders.foreach {
          case (out, asset) =>
            assetRepo.insert(asset).runWithIO()
            outputRepo.insert(out).runWithIO()
        }

        sellOrders.foreach {
          case (out, asset) =>
            val expectedOuts = List(ExtendedOutput(out, None))
            outputRepo
              .getAllMainUnspentSellOrderByTokenId(
                asset.tokenId,
                contractTemplate,
                0,
                Int.MaxValue
              )
              .compile
              .toList
              .runWithIO() should contain theSameElementsAs expectedOuts
        }

        outputRepo
          .getAllMainUnspentSellOrderByTokenId(
            arbTokenId,
            contractTemplate,
            0,
            Int.MaxValue
          )
          .compile
          .toList
          .runWithIO() shouldBe empty
      }
    }
  }

  property("insert/getUnspentBuyOrders") {
    withLiveRepos[ConnectionIO] { (_, _, outputRepo, _) =>
      forSingleInstance(dexBuyOrderGen) { buyOrder =>
        val contractTemplate = dex.buyContractTemplate[IO].unsafeRunSync()
        val arbTokenId       = assetIdGen.retryUntil(_ => true).sample.get
        outputRepo
          .getAllMainUnspentBuyOrderByTokenId(
            arbTokenId,
            contractTemplate,
            0,
            Int.MaxValue
          )
          .compile
          .toList
          .runWithIO() shouldBe empty

        outputRepo.insert(buyOrder).runWithIO()

        val tokenEmbeddedInContract =
          TokenId
            .fromString[Try](
              "21f84cf457802e66fb5930fb5d45fbe955933dc16a72089bf8980797f24e2fa1"
            )
            .get
        val expectedOuts = List(ExtendedOutput(buyOrder, None))

        outputRepo
          .getAllMainUnspentBuyOrderByTokenId(
            tokenEmbeddedInContract,
            contractTemplate,
            0,
            Int.MaxValue
          )
          .compile
          .toList
          .runWithIO() should contain theSameElementsAs expectedOuts

        outputRepo
          .getAllMainUnspentBuyOrderByTokenId(
            arbTokenId,
            contractTemplate,
            0,
            Int.MaxValue
          )
          .compile
          .toList
          .runWithIO() shouldBe empty
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      HeaderRepo[D],
      TransactionRepo[D, fs2.Stream],
      OutputRepo[D, fs2.Stream],
      AssetRepo[D, fs2.Stream]
    ) => Any
  ): Any = {
    val headerRepo = repositories.HeaderRepo[D]
    val txRepo     = repositories.TransactionRepo[D]
    val outRepo    = repositories.OutputRepo[D]
    val assetRepo  = repositories.AssetRepo[D]
    body(headerRepo, txRepo, outRepo, assetRepo)
  }
}
