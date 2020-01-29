package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.ConnectionIO
import org.ergoplatform.explorer.{db, BoxId}
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.syntax.runConnectionIO._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssetRepoSpec
  extends PropSpec
  with Matchers
  with RealDbTest
  with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getAllByBoxId") {
    withLiveRepo[ConnectionIO] { repo =>
      forSingleInstance(assetsWithBoxIdGen) {
        case (boxId, assets) =>
          repo.getAllByBoxId(boxId).runWithIO() shouldBe 'empty
          assets.foreach { asset =>
            repo.insert(asset).runWithIO()
          }
          repo
            .getAllByBoxId(boxId)
            .runWithIO() should contain theSameElementsAs assets
      }
    }
  }

  property("insert/getIssuingBoxes/getAllIssuingBoxes") {
    withLiveRepos[ConnectionIO] { (assetRepo, outputRepo, inputRepo) =>
      forSingleInstance(issueTokensGen(5)) { issuedTokens =>
        assetRepo.getAllIssuingBoxes.compile.toList.runWithIO() shouldBe empty
        val issuedTokenIds = NonEmptyList.fromList(issuedTokens.map(_._3.tokenId)).get
        assetRepo.getIssuingBoxes(issuedTokenIds).runWithIO() shouldBe empty

        issuedTokens.foreach {
          case (input, out, token) =>
            // issue a token
            inputRepo.insert(input).runWithIO()
            assetRepo.insert(token).runWithIO()
            outputRepo.insert(out).runWithIO()
            // use issued token in new output
            val outUsingToken = outputGen(true).sample.get
            assetRepo.insert(token.copy(boxId = outUsingToken.boxId)).runWithIO()
            outputRepo.insert(outUsingToken).runWithIO()
        }

        val outsIssuingToken = issuedTokens.map(_._2).map(ExtendedOutput(_, None))

        assetRepo.getAllIssuingBoxes.compile.toList
          .runWithIO() should contain theSameElementsAs outsIssuingToken

        // test all
        assetRepo
          .getIssuingBoxes(issuedTokenIds)
          .runWithIO() should contain theSameElementsAs outsIssuingToken

        // test one
        assetRepo
          .getIssuingBoxes(NonEmptyList.one(issuedTokenIds.head))
          .runWithIO() should contain theSameElementsAs List(outsIssuingToken.head)

        // test many
        assetRepo
          .getIssuingBoxes(NonEmptyList.fromList(issuedTokenIds.tail).get)
          .runWithIO() should contain theSameElementsAs outsIssuingToken.tail
      }
    }
  }

  private def withLiveRepo[D[_]: LiftConnectionIO: Sync](
    body: AssetRepo[D, fs2.Stream] => Any
  ): Any =
    body(db.repositories.AssetRepo[D])

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (AssetRepo[D, fs2.Stream], OutputRepo[D, fs2.Stream], InputRepo[D]) => Any
  ): Any =
    body(
      db.repositories.AssetRepo[D],
      db.repositories.OutputRepo[D],
      db.repositories.InputRepo[D]
    )

}
