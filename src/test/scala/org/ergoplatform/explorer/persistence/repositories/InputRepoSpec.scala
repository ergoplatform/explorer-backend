package org.ergoplatform.explorer.persistence.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import org.ergoplatform.explorer.persistence.{repositories, RealDbTest}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class InputRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("insert/getAllByTxId") {
    withLiveRepos { (outRepo, inRepo) =>
      forSingleInstance(extInputWithOutputGen()) {
        case (out, input) =>
          println(input)
          outRepo.insert(out).unsafeRunSync()
          inRepo.getAllByTxId(input.input.txId).unsafeRunSync() shouldBe 'empty
          inRepo.insert(input.input).unsafeRunSync()
          inRepo.getAllByTxId(input.input.txId).unsafeRunSync() shouldBe List(input)
      }
    }
  }

  property("getAllByTxIds") {
    withLiveRepos { (outRepo, inRepo) =>
      forSingleInstance(Gen.listOfN(5, extInputWithOutputGen())) { outputsWithInputs =>
        outputsWithInputs.foreach {
          case (out, in) =>
            outRepo.insert(out).unsafeRunSync()
            inRepo.insert(in.input).unsafeRunSync()
        }
        val ids = NonEmptyList.fromList(outputsWithInputs.map(_._2.input.txId)).get
        inRepo
          .getAllByTxIds(ids)
          .unsafeRunSync() should contain theSameElementsAs outputsWithInputs.map(_._2)
      }
    }
  }

  private def withLiveRepos(
    body: (OutputRepo.Live[IO], InputRepo.Live[IO]) => Any
  ): Any = {
    val outRepo = new repositories.OutputRepo.Live[IO](xa)
    val inRepo = new repositories.InputRepo.Live[IO](xa)
    body(outRepo, inRepo)
  }
}
