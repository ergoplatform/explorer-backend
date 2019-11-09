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
      withSingleInstance(outputsWithTxWithHeaderGen(mainChain = true)) {
        case (header, tx, outputs) =>
          hRepo.insert(header).unsafeRunSync()
          txRepo.insert(tx).unsafeRunSync()
          outputs.foreach {
            case (out, extOut) =>
              oRepo.getByBoxId(out.boxId).unsafeRunSync() shouldBe None
              oRepo.insert(out)
              oRepo.getByBoxId(out.boxId).unsafeRunSync() shouldBe Some(extOut)
          }
      }
    }
  }

  property("getAllByAddress/getAllByErgoTree") {
    withLiveRepos { (hRepo, txRepo, oRepo) =>
      withSingleInstance(hexStringRGen.flatMap(hex => addressGen.map(_ -> hex))) {
        case (address, ergoTree) =>
          withSingleInstance(outputsWithTxWithHeaderGen(mainChain = true)) {
            case (header, tx, outputs) =>
              hRepo.insert(header).unsafeRunSync()
              txRepo.insert(tx).unsafeRunSync()
              val (matching, nonMatching) = (outputs.tail, outputs.last)
              matching
                .map {
                  case (out, extOut) =>
                    out.copy(address = address, ergoTree = ergoTree) -> extOut
                }
                .foreach {
                  case (out, _) =>
                    oRepo.insert(out).unsafeRunSync()
                }
              oRepo.insert(nonMatching._1).unsafeRunSync()
              oRepo
                .getAllByAddress(address)
                .compile
                .toList
                .unsafeRunSync() should contain theSameElementsAs matching.map(_._2)
              oRepo
                .getAllByErgoTree(ergoTree)
                .compile
                .toList
                .unsafeRunSync() should contain theSameElementsAs matching.map(_._2)
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
