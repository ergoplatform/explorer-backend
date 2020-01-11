package org.ergoplatform.explorer.protocol.models

import io.circe.Decoder
import org.ergoplatform.explorer.TxId

/** A model mirroring ErgoTransaction entity from Ergo node REST API.
 * See `ErgoTransaction` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
 */
final case class ApiTransaction(
  id: TxId,
  inputs: List[ApiInput],
  outputs: List[ApiOutput],
  size: Int
)

object ApiTransaction {

  implicit val decoder: Decoder[ApiTransaction] = { c =>
    for {
      id      <- c.downField("id").as[TxId]
      inputs  <- c.downField("inputs").as[List[ApiInput]]
      outputs <- c.downField("outputs").as[List[ApiOutput]]
      size    <- c.downField("size").as[Int]
    } yield ApiTransaction(id, inputs, outputs, size)
  }
}
