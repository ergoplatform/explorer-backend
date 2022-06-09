package org.ergoplatform.explorer.http.api.v1.utils

import cats.syntax.option._
import org.ergoplatform.explorer.db.models.{BlockedToken, GenuineToken}
import org.ergoplatform.explorer.http.api.v1.TokenStatus
import org.ergoplatform.explorer.http.api.v1.TokenStatus.TokenStatus.{Blocked, Null, Suspicious, Unknown, Verified}

object TokenVerificationOptionT {

  // check if token is listed in GenuineTokenRepo
  def isVerified(genuineT: Option[GenuineToken]): Option[TokenStatus] =
    genuineT
      .map(_ => Verified)
      .orElse(Null.some)

  // check if token is listed in BlockedTokenRepo
  def isBlocked(blockedT: Option[BlockedToken]): Option[TokenStatus] =
    blockedT
      .map(_ => Blocked)
      .orElse(Null.some)

  // assume token is not listed in Blocked/Genuine-TokenRepo
  // if genuineL has just one element; token is suspicious else unknown
  def isSuspicious(genuineTs: Option[List[GenuineToken]]): Option[TokenStatus] =
    genuineTs
      .map(_ => Suspicious)
      .orElse(Null.some)

  def apply(
    genuineT: Option[GenuineToken],
    blockedT: Option[BlockedToken],
    genuineTs: Option[List[GenuineToken]]
  ): Option[TokenStatus] =
    (for {
      verifiedState <- isVerified(genuineT).map(_.value) // verified
      blockedState  <- isBlocked(blockedT).map(_.value) // blocked
      susState      <- isSuspicious(genuineTs).map(_.value) // suspicious
    } yield
      if (verifiedState > -1) Math.max(verifiedState, blockedState)
      else Math.max(susState, blockedState))
      .map(state => if (state > -1) state else Unknown.value)
      .map(x => TokenStatus.parse(x))
}
