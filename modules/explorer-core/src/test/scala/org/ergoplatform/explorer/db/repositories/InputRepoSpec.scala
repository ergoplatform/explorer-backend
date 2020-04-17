package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class InputRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getAllByTxId") {
    withLiveRepos[ConnectionIO] { (outRepo, inRepo) =>
      forSingleInstance(extInputWithOutputGen()) {
        case (out, input) =>
          outRepo.insert(out).runWithIO()
          inRepo.getAllByTxId(input.input.txId).runWithIO() shouldBe 'empty
          inRepo.insert(input.input).runWithIO()
          inRepo.getAllByTxId(input.input.txId).runWithIO() shouldBe List(input)
      }
    }
  }

  property("getAllByTxIds") {
    withLiveRepos[ConnectionIO] { (outRepo, inRepo) =>
      forSingleInstance(Gen.listOfN(5, extInputWithOutputGen())) { outputsWithInputs =>
        outputsWithInputs.foreach {
          case (out, in) =>
            outRepo.insert(out).runWithIO()
            inRepo.insert(in.input).runWithIO()
        }
        val ids = NonEmptyList.fromList(outputsWithInputs.map(_._2.input.txId)).get
        inRepo
          .getAllByTxIds(ids)
          .runWithIO() should contain theSameElementsAs outputsWithInputs.map(_._2)
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (OutputRepo[D, fs2.Stream], InputRepo[D]) => Any
  ): Any =
    body(
      repositories.OutputRepo[IO, D].unsafeRunSync(),
      repositories.InputRepo[IO, D].unsafeRunSync()
    )
}
