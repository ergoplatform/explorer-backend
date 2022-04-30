package org.ergoplatform.explorer.http.api.v1.utils

import cats.Monad
import cats.syntax.option._
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{BlockedToken, GenuineToken}
import tofu.syntax.monadic._

object TokenVerificationD {
  val verified = 1
  val blocked  = 3
  val sus      = 2
  val unknown  = 0

  // check if token is listed in GenuineTokenRepo
  def isVerified[D[_]: LiftConnectionIO: Monad](id: TokenId, getGenuine: TokenId => D[Option[GenuineToken]]): D[Int] =
    getGenuine(id).map(_.map(_ => verified).getOrElse(Int.MinValue))

  // check if token is listed in BlockedTokenRepo
  def isBlocked[D[_]: LiftConnectionIO: Monad](id: TokenId, getBlocked: TokenId => D[Option[BlockedToken]]): D[Int] =
    getBlocked(id).map(_.map(_ => blocked).getOrElse(Int.MinValue))

  // assume tokenId is not listed in Blocked/Genuine-TokenRepo
  // if getGenuineByNameAndUnique has more than one; token is suspicious else unknown
  def isSus[D[_]: LiftConnectionIO: Monad](
    tokenName: String,
    getGenuineByNameAndUnique: (String, Boolean) => D[List[GenuineToken]]
  ): D[Int] =
    getGenuineByNameAndUnique(tokenName, true).map(x => if (x.tail.isEmpty) unknown else sus)

  def apply[D[_]: LiftConnectionIO: Monad](
    id: TokenId,
    tokenName: String,
    getGenuine: TokenId => D[Option[GenuineToken]],
    getBlocked: TokenId => D[Option[BlockedToken]],
    getGenuineByNameAndUnique: (String, Boolean) => D[List[GenuineToken]]
  ): D[Int] = for {
    verified <- isVerified(id, getGenuine) // verified
    blocked  <- isBlocked(id, getBlocked) // blocked
    sus      <- isSus(tokenName, getGenuineByNameAndUnique) // suspicious or unknown
  } yield
    if (verified > Int.MinValue) Math.max(verified, blocked)
    else Math.max(sus, blocked)

}

object TokenVerificationOptionT {
  val verified = 1
  val blocked  = 3
  val sus      = 2
  val unknown  = 0

  // check if token is listed in GenuineTokenRepo
  def isVerified(genuineT: Option[GenuineToken]): Option[Int] =
    genuineT.map(_ => verified).orElse(Int.MinValue.some)

  // check if token is listed in BlockedTokenRepo
  def isBlocked(blockedT: Option[BlockedToken]): Option[Int] =
    blockedT.map(_ => blocked).orElse(Int.MinValue.some)

  // assume token is not listed in Blocked/Genuine-TokenRepo
  // if genuineL has just one element; token is suspicious else unknown
  def isSus(genuineTs: Option[List[GenuineToken]]): Option[Int] =
    genuineTs
      .map(_ => sus)
      .orElse(
        Int.MinValue.some
      )

  def apply(
    genuineT: Option[GenuineToken],
    blockedT: Option[BlockedToken],
    genuineTs: Option[List[GenuineToken]]
  ): Option[Int] =
    (for {
      verifiedState <- isVerified(genuineT) // verified
      blockedState  <- isBlocked(blockedT) // blocked
      susState      <- isSus(genuineTs) // suspicious
    } yield
      if (verifiedState > Int.MinValue) Math.max(verifiedState, blockedState)
      else Math.max(susState, blockedState)).map(state => if (state > Int.MinValue) state else unknown)
}
