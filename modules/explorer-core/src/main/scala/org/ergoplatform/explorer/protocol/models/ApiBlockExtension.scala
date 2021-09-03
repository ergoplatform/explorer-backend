package org.ergoplatform.explorer.protocol.models

import derevo.circe.decoder
import derevo.derive
import io.circe.Json
import io.circe.refined._
import org.ergoplatform.explorer.{HexString, BlockId}

/** A model mirroring Extension entity from Ergo node REST API.
  * See `Extension` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(decoder)
final case class ApiBlockExtension(
                                    headerId: BlockId,
                                    digest: HexString,
                                    fields: Json
)
