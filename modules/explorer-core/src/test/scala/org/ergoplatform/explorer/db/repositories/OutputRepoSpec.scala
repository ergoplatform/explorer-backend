package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import cats.instances.try_._
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._

import org.scalatest._
import flatspec._
import matchers._

import scala.util.Try

class OutputRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "OutputRepo" should "insert/getByBoxId" in {
    withLiveRepos[ConnectionIO] { (hRepo, txRepo, oRepo, _) =>
      forSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) { case (header, tx, outputs) =>
        hRepo.insert(header).runWithIO()
        txRepo.insert(tx).runWithIO()
        outputs.foreach { extOut =>
          oRepo.getByBoxId(extOut.output.boxId).runWithIO() should be(None)
          oRepo.insert(extOut.output).runWithIO()
          oRepo.getByBoxId(extOut.output.boxId).runWithIO() should be(Some(extOut))
        }
      }
    }
  }

  it should "getAllByAddress/getAllByErgoTree" in {
    withLiveRepos[ConnectionIO] { (hRepo, txRepo, oRepo, _) =>
      forSingleInstance(hexStringRGen.flatMap(hex => addressGen.map(_ -> hex))) { case (address, ergoTree) =>
        forSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) { case (header, tx, outputs) =>
          hRepo.insert(header).runWithIO()
          txRepo.insert(tx).runWithIO()
          val nonMatching = outputs.head
          val matching = outputs.tail
            .map { extOut =>
              extOut.copy(
                output = extOut.output.copy(address = address, ergoTree = ergoTree)
              )
            }
          matching.foreach { extOut =>
            oRepo.insert(extOut.output).runWithIO()
          }
          oRepo.insert(nonMatching.output).runWithIO()
          oRepo
            .streamAllByErgoTree(ergoTree, 0, Int.MaxValue)
            .compile
            .toList
            .runWithIO() should contain theSameElementsAs matching
        }
      }
    }
  }



  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      HeaderRepo[D, fs2.Stream],
      TransactionRepo[D, fs2.Stream],
      OutputRepo[D, fs2.Stream],
      AssetRepo[D, fs2.Stream]
    ) => Any
  ): Any = {
    val headerRepo = repositories.HeaderRepo[IO, D].unsafeRunSync()
    val txRepo     = repositories.TransactionRepo[IO, D].unsafeRunSync()
    val outRepo    = repositories.OutputRepo[IO, D].unsafeRunSync()
    val assetRepo  = repositories.AssetRepo[IO, D].unsafeRunSync()
    body(headerRepo, txRepo, outRepo, assetRepo)
  }
}
