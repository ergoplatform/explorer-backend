package org.ergoplatform.explorer.http.api.v1.utils

import cats.Monad
import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.{BlockedToken, GenuineToken}
import tofu.syntax.monadic._

object TokenVerification {
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
  // check if tokenName is the same as a verified token with unique-name
  def isSus[D[_]: LiftConnectionIO: Monad](
    tokenName: String,
    getGenuineByNameAndUnique: (String, Boolean) => D[Option[GenuineToken]]
  ): D[Int] =
    getGenuineByNameAndUnique(tokenName, true).map(_.map(_ => sus).getOrElse(unknown))

  def apply[D[_]: LiftConnectionIO: Monad](
    id: TokenId,
    tokenName: String,
    getGenuine: TokenId => D[Option[GenuineToken]],
    getBlocked: TokenId => D[Option[BlockedToken]],
    getGenuineByNameAndUnique: (String, Boolean) => D[Option[GenuineToken]]
  ): D[Int] = for {
    verified <- isVerified(id, getGenuine) // verified
    blocked  <- isBlocked(id, getBlocked) // blocked
    sus      <- isSus(tokenName, getGenuineByNameAndUnique) // suspicious or unknown
  } yield
    if (verified > Int.MinValue) Math.max(verified, blocked)
    else Math.max(sus, blocked)

}
