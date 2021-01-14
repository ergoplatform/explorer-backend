package org.ergoplatform.explorer.db.repositories

import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import doobie.ConnectionIO
import org.ergoplatform.explorer.db
import org.ergoplatform.explorer.db.RealDbTest
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AssetRepoSpec extends PropSpec with Matchers with RealDbTest with ScalaCheckDrivenPropertyChecks {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  property("insert/getAllByBoxId") {
    withLiveRepo[ConnectionIO] { repo =>
      forSingleInstance(assetsWithBoxIdGen) { case (boxId, assets) =>
        repo.getAllByBoxId(boxId).runWithIO() shouldBe 'empty
        assets.foreach { asset =>
          repo.insert(asset).runWithIO()
        }
        val eAssets =
          assets.map(a => ExtendedAsset(a.tokenId, a.boxId, a.headerId, a.index, a.amount, None, None, None))
        repo
          .getAllByBoxId(boxId)
          .runWithIO() should contain theSameElementsAs eAssets
      }
    }
  }

  property("insert/getIssuingBoxes/getAllIssuingBoxes") {
    withLiveRepos[ConnectionIO] { (assetRepo, outputRepo, inputRepo) =>
      forSingleInstance(issueTokensGen(5)) { issuedTokens =>
        assetRepo.getAllIssuingBoxes(0, Int.MaxValue).runWithIO() shouldBe empty
        val issuedTokenIds = NonEmptyList.fromList(issuedTokens.map(_._3.tokenId)).get
        assetRepo.getIssuingBoxesByTokenIds(issuedTokenIds).runWithIO() shouldBe empty

        issuedTokens.foreach { case (input, out, token) =>
          // issue a token
          inputRepo.insert(input).runWithIO()
          assetRepo.insert(token).runWithIO()
          outputRepo.insert(out).runWithIO()
          // use issued token in new output
          val outUsingToken = outputGen(true).retryUntil(_ => true).sample.get
          assetRepo.insert(token.copy(boxId = outUsingToken.boxId)).runWithIO()
          outputRepo.insert(outUsingToken).runWithIO()
        }

        val outsIssuingToken = issuedTokens.map(_._2).map(ExtendedOutput(_, None))

        assetRepo
          .getAllIssuingBoxes(0, Int.MaxValue)
          .runWithIO() should contain theSameElementsAs outsIssuingToken

        // test all
        assetRepo
          .getIssuingBoxesByTokenIds(issuedTokenIds)
          .runWithIO() should contain theSameElementsAs outsIssuingToken

        // test one
        assetRepo
          .getIssuingBoxesByTokenIds(NonEmptyList.one(issuedTokenIds.head))
          .runWithIO() should contain theSameElementsAs List(outsIssuingToken.head)

        // test many
        assetRepo
          .getIssuingBoxesByTokenIds(NonEmptyList.fromList(issuedTokenIds.tail).get)
          .runWithIO() should contain theSameElementsAs outsIssuingToken.tail
      }
    }
  }

  private def withLiveRepo[D[_]: LiftConnectionIO: Sync](
    body: AssetRepo[D, fs2.Stream] => Any
  ): Any =
    body(db.repositories.AssetRepo[IO, D].unsafeRunSync())

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (AssetRepo[D, fs2.Stream], OutputRepo[D, fs2.Stream], InputRepo[D]) => Any
  ): Any =
    body(
      db.repositories.AssetRepo[IO, D].unsafeRunSync(),
      db.repositories.OutputRepo[IO, D].unsafeRunSync(),
      db.repositories.InputRepo[IO, D].unsafeRunSync()
    )

}
