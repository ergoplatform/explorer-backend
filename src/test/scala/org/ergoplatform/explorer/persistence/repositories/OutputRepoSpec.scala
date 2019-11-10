package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import org.ergoplatform.explorer.persistence.{repositories, RealDbTest}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class OutputRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert/getByBoxId") {
    withLiveRepos { (hRepo, txRepo, oRepo) =>
      withSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) {
        case (header, tx, outputs) =>
          hRepo.insert(header).unsafeRunSync()
          txRepo.insert(tx).unsafeRunSync()
          outputs.foreach { extOut =>
            oRepo.getByBoxId(extOut.output.boxId).unsafeRunSync() shouldBe None
            oRepo.insert(extOut.output).unsafeRunSync()
            oRepo.getByBoxId(extOut.output.boxId).unsafeRunSync() shouldBe Some(extOut)
          }
      }
    }
  }

  property("getAllByAddress/getAllByErgoTree") {
    withLiveRepos { (hRepo, txRepo, oRepo) =>
      withSingleInstance(hexStringRGen.flatMap(hex => addressGen.map(_ -> hex))) {
        case (address, ergoTree) =>
          withSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) {
            case (header, tx, outputs) =>
              hRepo.insert(header).unsafeRunSync()
              txRepo.insert(tx).unsafeRunSync()
              val dOutputs = outputs.distinct
              val (matching, nonMatching) = (dOutputs.tail, dOutputs.last)
              matching
                .map { extOut =>
                  extOut.copy(
                    output = extOut.output.copy(address = address, ergoTree = ergoTree)
                  )
                }
                .foreach { extOut =>
                  oRepo.insert(extOut.output).unsafeRunSync()
                }
              oRepo.insert(nonMatching.output).unsafeRunSync()
              oRepo
                .getAllByAddress(address)
                .compile
                .toList
                .unsafeRunSync() should contain theSameElementsAs matching
              oRepo
                .getAllByErgoTree(ergoTree)
                .compile
                .toList
                .unsafeRunSync() should contain theSameElementsAs matching
          }
      }
    }
  }

  private def withLiveRepos(
    body: (HeaderRepo.Live[IO], TransactionRepo.Live[IO], OutputRepo.Live[IO]) => Any
  ): Any = {
    val headerRepo = new repositories.HeaderRepo.Live[IO](xa)
    val txRepo = new repositories.TransactionRepo.Live[IO](xa)
    val outRepo = new repositories.OutputRepo.Live[IO](xa)
    body(headerRepo, txRepo, outRepo)
  }
}
