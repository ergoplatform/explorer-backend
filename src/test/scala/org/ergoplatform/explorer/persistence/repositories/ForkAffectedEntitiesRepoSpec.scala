package org.ergoplatform.explorer.persistence.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.BoxId
import org.ergoplatform.explorer.persistence.models.Input
import org.ergoplatform.explorer.persistence.{repositories, RealDbTest}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.ergoplatform.explorer.persistence.doobieInstances._

class ForkAffectedEntitiesRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.persistence.models.Generators._

  property("updateChainStatusByHeaderId") {
    val hRepo = new repositories.HeaderRepo.Live[IO](xa)
    val txRepo = new repositories.TransactionRepo.Live[IO](xa)
    val inRepo = new repositories.InputRepo.Live[IO](xa)
    val outRepo = new repositories.OutputRepo.Live[IO](xa)
    val repo = new repositories.ForkAffectedEntitiesRepo.Live[IO](xa)

    forSingleInstance(fullBlockGen(mainChain = true)) {
      case (header, txs, inputs, outputs) =>
        hRepo.insert(header).unsafeRunSync()
        txs.foreach(tx => txRepo.insert(tx).unsafeRunSync())
        inputs.foreach(in => inRepo.insert(in).unsafeRunSync())
        outputs.foreach(out => outRepo.insert(out).unsafeRunSync())

        repo
          .updateChainStatusByHeaderId(header.id)(newChainStatus = false)
          .unsafeRunSync()

        hRepo.get(header.id)
          .unsafeRunSync()
          .map(_.mainChain) shouldBe Some(false)
        inputs.foreach(
          in =>
            genInputByBoxId(in.boxId)
              .unsafeRunSync()
              .map(_.mainChain) shouldBe Some(false)
        )
        outputs.foreach(
          out =>
            outRepo
              .getByBoxId(out.boxId)
              .unsafeRunSync()
              .map(_.output.mainChain) shouldBe Some(false)
        )
    }
  }

  private def genInputByBoxId(boxId: BoxId): IO[Option[Input]] =
    sql"select * from node_inputs where box_id = $boxId"
      .query[Input]
      .option
      .transact(xa)

}
