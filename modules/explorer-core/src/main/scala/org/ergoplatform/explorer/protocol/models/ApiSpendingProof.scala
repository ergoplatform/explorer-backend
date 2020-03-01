package org.ergoplatform.explorer.protocol.models

import cats.instances.either._
import cats.syntax.option._
import io.circe.refined._
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import org.ergoplatform.explorer.HexString

/** A model mirroring SpendingProof entity from Ergo node REST API.
  * See `SpendingProof` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiSpendingProof(proofBytes: Option[HexString], extension: Json)

object ApiSpendingProof {

  implicit val decoder: Decoder[ApiSpendingProof] = { c: HCursor =>
    for {
      // here decoding of refined type field value has to be handled manually as node API
      // may return an empty string (instead of `null`) which fails the refinement.
      proofBytes <- c.downField("proofBytes").as[String].flatMap { s =>
                      // todo: Simplify when node API is improved.
                      HexString.fromString[Either[Throwable, *]](s) match {
                        case Left(_) => Right[DecodingFailure, Option[HexString]](none)
                        case r @ Right(_) =>
                          r.asInstanceOf[Decoder.Result[HexString]].map(_.some)
                      }
                    }
      extension  <- c.downField("extension").as[Json]
    } yield ApiSpendingProof(proofBytes, extension)
  }
}
