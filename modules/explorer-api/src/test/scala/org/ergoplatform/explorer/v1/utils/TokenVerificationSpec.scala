package org.ergoplatform.explorer.v1.utils

import cats.effect.{IO, Sync}
import cats.syntax.option._
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.TokenName
import org.ergoplatform.explorer.http.api.v1.TokenStatus.TokenStatus.{Blocked, Suspicious, Unknown, Verified}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{BlockedToken, GenuineToken}
import org.ergoplatform.explorer.db.repositories.{BlockedTokenRepo, GenuineTokenRepo}
import org.ergoplatform.explorer.http.api.v1.utils.TokenVerificationOptionT._
import org.ergoplatform.explorer.http.api.v1.utils.TokenVerificationOptionT
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TokenVerificationSpec extends AnyFlatSpec with should.Matchers with RealDbTest {
  import TokenVerificationSpec._
  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "TokenVerification" should "return correct [genuine] integer value for GenuineTokens" in {
    withGenuineTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(genuineTokenGen("TOKEN#1", uniqueName = true)) { genuineToken =>
        repo.insert(genuineToken).runWithIO()
        val gtOps = repo.get(genuineToken.id).runWithIO()
        gtOps should be(Some(genuineToken))
        isVerified(gtOps) should be(Verified.some)
      }
    }
  }

  it should "return correct [genuine] integer value for BlockedTokens " in {
    withBlockedTokenRepo[ConnectionIO] { repo =>
      forSingleInstance(blockedTokenGen) { blockedToken =>
        repo.insert(blockedToken).runWithIO()
        val btOPs = repo.get(blockedToken.tokenId).runWithIO()
        btOPs should be(Some(blockedToken))
        isBlocked(btOPs) should be(Blocked.some)
      }
    }
  }

  // suspicious token mimics a token in GenuineTokenList
  it should "return correct [genuine] integer value for SuspiciousTokens " in {
    withGenuineTokenRepo[ConnectionIO] { repo =>
      val tokenName = "TOKEN#1A"
      forSingleInstance(genuineTokenGen(tokenName, uniqueName = true)) { genuineToken =>
        repo.insert(genuineToken).runWithIO()
        repo.get(genuineToken.id).runWithIO() should be(Some(genuineToken))
        val gTsOps = repo.getByNameAndUnique(TokenName(tokenName), unique = true).runWithIO()
        isSuspicious(gTsOps.some) should be(Suspicious.some)
      }
    }
  }

  it should "correctly check GenuineTokens" in {
    withLiveRepos[ConnectionIO] { (genuineTokenRepo, blockedTokenRepo) =>
      val token1Name = "TOKEN#1B"
      val gens = for {
        gTs <- genuineTokenListGen(List((token1Name, true), ("TOKEN#2B", true)))
        bT  <- blockedTokenGen
      } yield (gTs, bT)
      forSingleInstance(gens) { case (gTs, bT) =>
        val t1 = gTs.filter(_.tokenName == token1Name).head
        gTs.foreach(genuineTokenRepo.insert(_).runWithIO())
        blockedTokenRepo.insert(bT).runWithIO()

        val genuineT: Option[GenuineToken] = genuineTokenRepo.get(t1.id).runWithIO()
        val blockedT: Option[BlockedToken] = blockedTokenRepo.get(t1.id).runWithIO()
        val genuineTs: Option[List[GenuineToken]] =
          genuineTokenRepo.getByNameAndUnique(TokenName(token1Name), unique = true).runWithIO().some

        TokenVerificationOptionT(genuineT, blockedT, genuineTs) should be(Verified.some)
      }
    }
  }

  it should "correctly check BlockedTokens" in {
    withLiveRepos[ConnectionIO] { (genuineTokenRepo, blockedTokenRepo) =>
      val gens = for {
        gT <- genuineTokenGen("TOKEN#1C", uniqueName = true)
        bT <- blockedTokenGen
      } yield (gT, bT)
      forSingleInstance(gens) { case (gT, bT) =>
        genuineTokenRepo.insert(gT).runWithIO()
        blockedTokenRepo.insert(bT).runWithIO()

        val genuineT: Option[GenuineToken] = genuineTokenRepo.get(bT.tokenId).runWithIO()
        val blockedT: Option[BlockedToken] = blockedTokenRepo.get(bT.tokenId).runWithIO()
        val genuineTs: Option[List[GenuineToken]] =
          genuineTokenRepo.getByNameAndUnique(TokenName(bT.tokenName), unique = true).runWithIO().some

        TokenVerificationOptionT(genuineT, blockedT, genuineTs) should be(Blocked.some)
      }
    }
  }

  it should "correctly check BlockedTokens - Tokens in GenuineTokens & BlockedTokens are identified as blocked" in {
    withLiveRepos[ConnectionIO] { (genuineTokenRepo, blockedTokenRepo) =>
      val token1Name = "TOKEN#1D"
      val gens = for {
        gTs <- genuineTokenListGen(List((token1Name, true), ("TOKEN#2D", true)))
        bT  <- blockedTokenGen
      } yield (gTs, bT)
      forSingleInstance(gens) { case (gTs, bT) =>
        val t1 = gTs.filter(_.tokenName == token1Name).head
        gTs.foreach(genuineTokenRepo.insert(_).runWithIO())
        blockedTokenRepo.insert(bT).runWithIO()
        blockedTokenRepo.insert(BlockedToken(t1.id, t1.tokenName)).runWithIO() // insert GT into Blocked

        val genuineT: Option[GenuineToken] = genuineTokenRepo.get(t1.id).runWithIO()
        val blockedT: Option[BlockedToken] = blockedTokenRepo.get(t1.id).runWithIO()
        val genuineTs: Option[List[GenuineToken]] =
          genuineTokenRepo.getByNameAndUnique(TokenName(token1Name), unique = true).runWithIO().some

        TokenVerificationOptionT(genuineT, blockedT, genuineTs) should be(Blocked.some)
      }
    }
  }

  it should "correctly check SuspiciousTokens" in {
    withLiveRepos[ConnectionIO] { (genuineTokenRepo, blockedTokenRepo) =>
      val gens = for {
        gT <- genuineTokenGen("TOKEN#1E", uniqueName = true)
        bT <- blockedTokenGen
        sT <- `tokenName&IDGen`("TOKEN#1E") // AS Suspicious Token
      } yield (gT, bT, sT)
      forSingleInstance(gens) { case (gT, bT, sT) =>
        // suspicious token is not in the blockedLIST || genuineLIST & mimics the name ok a genuine token :)
        genuineTokenRepo.insert(gT).runWithIO()
        blockedTokenRepo.insert(bT).runWithIO()

        val genuineT: Option[GenuineToken] = genuineTokenRepo.get(sT._1).runWithIO()
        val blockedT: Option[BlockedToken] = blockedTokenRepo.get(sT._1).runWithIO()
        val genuineTs: Option[List[GenuineToken]] =
          genuineTokenRepo.getByNameAndUnique(TokenName(sT._2), unique = true).runWithIO().some

        TokenVerificationOptionT(genuineT, blockedT, genuineTs) should be(Suspicious.some)
      }
    }
  }

  it should "correctly check Unknown Tokens" in {
    withLiveRepos[ConnectionIO] { (genuineTokenRepo, blockedTokenRepo) =>
      val gens = for {
        gT <- genuineTokenGen("TOKEN#1F", uniqueName = true)
        bT <- blockedTokenGen
        sT <- `tokenName&IDGen`("TOKEN#2F") // AS UNKNOWN Token
      } yield (gT, bT, sT)
      forSingleInstance(gens) { case (gT, bT, sT) =>
        genuineTokenRepo.insert(gT).runWithIO()
        blockedTokenRepo.insert(bT).runWithIO()

        val genuineT: Option[GenuineToken] = genuineTokenRepo.get(sT._1).runWithIO()
        val blockedT: Option[BlockedToken] = blockedTokenRepo.get(sT._1).runWithIO()
        val genuineTs: Option[List[GenuineToken]] =
          genuineTokenRepo.getByNameAndUniqueOP(TokenName(sT._2), unique = true).runWithIO()

        println(s"genuineT: $genuineT")
        println(s"blockedT: $blockedT")
        println(s"genuineTs: $genuineTs")

        TokenVerificationOptionT(genuineT, blockedT, genuineTs) should be(Unknown.some)
      }
    }
  }

}

object TokenVerificationSpec {

  private def withGenuineTokenRepo[D[_]: LiftConnectionIO: Sync](
    body: GenuineTokenRepo[D] => Any
  ): Any =
    body(repositories.GenuineTokenRepo[IO, D].unsafeRunSync())

  private def withBlockedTokenRepo[D[_]: LiftConnectionIO: Sync](
    body: BlockedTokenRepo[D] => Any
  ): Any =
    body(repositories.BlockedTokenRepo[IO, D].unsafeRunSync())

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (GenuineTokenRepo[D], BlockedTokenRepo[D]) => Any
  ): Any =
    body(
      repositories.GenuineTokenRepo[IO, D].unsafeRunSync(),
      repositories.BlockedTokenRepo[IO, D].unsafeRunSync()
    )
}
