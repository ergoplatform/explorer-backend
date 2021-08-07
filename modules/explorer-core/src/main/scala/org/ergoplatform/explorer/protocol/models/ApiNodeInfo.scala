package org.ergoplatform.explorer.protocol.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.explorer.{BlockId, HexString}

/** A model mirroring NodeInfo entity from Ergo node REST API.
  * See `NodeInfo` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(encoder, decoder)
final case class ApiNodeInfo(
  currentTime: Long,
  name: String,
  stateType: String,
  difficulty: Long,
  bestFullHeaderId: BlockId,
  bestHeaderId: BlockId,
  peersCount: Int,
  unconfirmedCount: Int,
  appVersion: String,
  stateRoot: HexString,
  previousFullHeaderId: BlockId,
  fullHeight: Int,
  headersHeight: Int,
  stateVersion: HexString,
  launchTime: Long,
  parameters: ApiNodeInfoEpochParameters,
  isMining: Boolean
)
