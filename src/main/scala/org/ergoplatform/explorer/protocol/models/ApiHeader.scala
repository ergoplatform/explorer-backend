package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}
import io.circe.refined._
import org.ergoplatform.explorer.{HexString, Id}

final case class ApiHeader(
  id: Id,
  parentId: Id,
  version: Byte,
  height: Long,
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
  votes: String,
  mainChain: Boolean = true
)

object ApiHeader {

  implicit val decoder: Decoder[ApiHeader] = { c: HCursor =>
    for {
      id               <- c.downField("id").as[Id]
      parentId         <- c.downField("parentId").as[Id]
      version          <- c.downField("version").as[Byte]
      height           <- c.downField("height").as[Long]
      nBits            <- c.downField("nBits").as[Long]
      difficulty       <- c.downField("difficulty").as[ApiDifficulty]
      timestamp        <- c.downField("timestamp").as[Long]
      stateRoot        <- c.downField("stateRoot").as[HexString]
      adProofsRoot     <- c.downField("adProofsRoot").as[HexString]
      transactionsRoot <- c.downField("transactionsRoot").as[HexString]
      extensionHash    <- c.downField("extensionHash").as[HexString]
      powSolutions     <- c.downField("powSolutions").as[ApiPowSolutions]
      votes            <- c.downField("votes").as[String]
    } yield
      ApiHeader(
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
