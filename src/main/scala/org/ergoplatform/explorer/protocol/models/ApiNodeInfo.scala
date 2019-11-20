package org.ergoplatform.explorer.protocol.models

import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined._
import org.ergoplatform.explorer.{HexString, Id}

final case class ApiNodeInfo(
  currentTime: Long,
  name: String,
  stateType: String,
  difficulty: Long,
  bestFullHeaderId: Id,
  bestHeaderId: Id,
  peersCount: Int,
  unconfirmedCount: Int,
  appVersion: String,
  stateRoot: HexString,
  previousFullHeaderId: Id,
  fullHeight: Long,
  headersHeight: Long,
  stateVersion: HexString,
  launchTime: Long,
  isMining: Boolean
)

object ApiNodeInfo {

  implicit val decoder: Decoder[ApiNodeInfo] = deriveDecoder
}
