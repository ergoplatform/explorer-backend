package org.ergoplatform.explorer.services

import org.ergoplatform.explorer.commonGenerators
import org.ergoplatform.explorer.protocol.models.{ApiNodeInfo, ApiNodeInfoEpochParameters}
import org.scalacheck.Gen

object nodeApiGenerator {

  def generateNodeInfo: Gen[ApiNodeInfo] = {
    for {
      id <- commonGenerators.idGen
      hex <- commonGenerators.hexStringRGen
    } yield ApiNodeInfo(
      1L,
      "testNode",
      "stateType",
      1L,
      id,
      id,
      1,
      1,
      "appVersion",
      hex,
      id,
      1,
      1,
      hex,
      1L,
      ApiNodeInfoEpochParameters(
        1, 1, 1, 1, 1, 1: Byte, 1, 1, 1, 1
      ),
      isMining = false
    )
  }
}
