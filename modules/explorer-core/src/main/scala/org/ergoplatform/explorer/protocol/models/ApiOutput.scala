package org.ergoplatform.explorer.protocol.models

import derevo.circe.decoder
import derevo.derive
import io.circe.refined._
import org.ergoplatform.explorer.{BoxId, HexString, RegisterId}

/** A model mirroring ErgoTransactionOutput entity from Ergo node REST API.
  * See `ErgoTransactionOutput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(decoder)
final case class ApiOutput(
  boxId: BoxId,
  value: Long,
  creationHeight: Int,
  ergoTree: HexString,
  assets: List[ApiAsset],
  additionalRegisters: Map[RegisterId, HexString]
)
