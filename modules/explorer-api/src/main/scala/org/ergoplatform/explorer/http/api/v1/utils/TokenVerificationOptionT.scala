package org.ergoplatform.explorer.http.api.v1.utils

import cats.syntax.option._
import org.ergoplatform.explorer.db.models.{BlockedToken, GenuineToken}

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
