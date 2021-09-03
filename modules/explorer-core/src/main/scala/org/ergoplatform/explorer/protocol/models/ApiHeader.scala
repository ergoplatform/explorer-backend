package org.ergoplatform.explorer.protocol.models

import io.circe.refined._
import io.circe.{Decoder, HCursor}
import org.ergoplatform.explorer.{BlockId, HexString}

/** A model mirroring BlockHeader entity from Ergo node REST API.
  * See `BlockHeader` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class ApiHeader(
  id: BlockId,
  parentId: BlockId,
  version: Byte,
  height: Int,
  nBits: Long,
  difficulty: ApiDifficulty,
  timestamp: Long,
  stateRoot: HexString,
  adProofsRoot: HexString,
  transactionsRoot: HexString,
  extensionHash: HexString,
  minerPk: HexString,
  w: HexString,
  n: HexString,
  d: String,
  votes: String
)

object ApiHeader {

  implicit val decoder: Decoder[ApiHeader] = { c: HCursor =>
    for {
      id               <- c.downField("id").as[BlockId]
      parentId         <- c.downField("parentId").as[BlockId]
      version          <- c.downField("version").as[Byte]
      height           <- c.downField("height").as[Int]
      nBits            <- c.downField("nBits").as[Long]
      difficulty       <- c.downField("difficulty").as[ApiDifficulty]
      timestamp        <- c.downField("timestamp").as[Long]
      stateRoot        <- c.downField("stateRoot").as[HexString]
      adProofsRoot     <- c.downField("adProofsRoot").as[HexString]
      transactionsRoot <- c.downField("transactionsRoot").as[HexString]
      extensionHash    <- c.downField("extensionHash").as[HexString]
      powSolutions     <- c.downField("powSolutions").as[ApiPowSolutions]
      votes            <- c.downField("votes").as[String]
    } yield ApiHeader(
      id,
      parentId,
      version,
      height,
      nBits,
      difficulty,
      timestamp,
      stateRoot,
      adProofsRoot,
      transactionsRoot,
      extensionHash,
      powSolutions.pk,
      powSolutions.w,
      powSolutions.n,
      powSolutions.d,
      votes
    )
  }
}
