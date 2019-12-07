package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}
import io.circe.refined._
import org.ergoplatform.explorer.{HexString, Id}

/** A model mirroring AdProof entity from Ergo node REST API.
  * See `BlockADProofs` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiAdProof(
  headerId: Id,
  proofBytes: HexString,
  digest: HexString
)

object ApiAdProof {

  implicit val decoder: Decoder[ApiAdProof] = { c: HCursor =>
    for {
      headerId   <- c.downField("headerId").as[Id]
      proofBytes <- c.downField("proofBytes").as[HexString]
      digest     <- c.downField("digest").as[HexString]
    } yield ApiAdProof(headerId, proofBytes, digest)
  }
}
