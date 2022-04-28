package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.scalacheck.Gen

import org.scalatest._
import flatspec._
import matchers._

class InputRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "InputRepo" should "insert/getAllByTxId" in {
    withLiveRepos[ConnectionIO] { (outRepo, inRepo) =>
      forSingleInstance(extInputWithOutputGen()) { case (out, input) =>
        outRepo.insert(out).runWithIO()
        inRepo.getAllByTxId(input.input.txId).runWithIO() should be('empty)
        inRepo.insert(input.input).runWithIO()
        inRepo.getAllByTxId(input.input.txId).runWithIO() should be(List(input))
      }
    }
  }

  it should "getAllByTxIds" in {
    withLiveRepos[ConnectionIO] { (outRepo, inRepo) =>
      forSingleInstance(Gen.listOfN(5, extInputWithOutputGen())) { outputsWithInputs =>
        outputsWithInputs.foreach { case (out, in) =>
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
