package org.ergoplatform.explorer.protocol.models

import derevo.circe.decoder
import derevo.derive
import io.circe.refined._
import org.ergoplatform.explorer.{BlockId, HexString}

/** A model mirroring AdProof entity from Ergo node REST API.
  * See `BlockADProofs` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(decoder)
final case class ApiAdProof(
  headerId: BlockId,
  proofBytes: HexString,
  digest: HexString
)
