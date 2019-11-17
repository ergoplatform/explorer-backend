package org.ergoplatform.explorer.protocol.models

import io.circe._
import io.circe.generic.semiauto._

final case class ApiNodeInfo(
  currentTime: Long,
  name: String,
  stateType: String,
  difficulty: Long,
  bestFullHeaderId: String,
  bestHeaderId: String,
  peersCount: Int,
  unconfirmedCount: Int,
  appVersion: String,
  stateRoot: String,
  previousFullHeaderId: String,
  fullHeight: Long,
  headersHeight: Long,
  stateVersion: String,
  launchTime: Long,
  isMining: Boolean
)

object ApiNodeInfo {

  implicit val decoder: Decoder[ApiNodeInfo] = deriveDecoder
}
