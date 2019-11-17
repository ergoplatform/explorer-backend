package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, HCursor}

final case class ApiHeader(
  id: String,
  parentId: String,
  version: Byte,
  height: Long,
  nBits: Long,
  difficulty: ApiDifficulty,
  timestamp: Long,
  stateRoot: String,
  adProofsRoot: String,
  transactionsRoot: String,
  extensionHash: String,
  minerPk: String,
  w: String,
  n: String,
  d: String,
  votes: String,
  mainChain: Boolean = true
)

object ApiHeader {

  implicit val decoder: Decoder[ApiHeader] = { c: HCursor =>
    for {
      id               <- c.downField("id").as[String]
      parentId         <- c.downField("parentId").as[String]
      version          <- c.downField("version").as[Byte]
      height           <- c.downField("height").as[Long]
      nBits            <- c.downField("nBits").as[Long]
      difficulty       <- c.downField("difficulty").as[ApiDifficulty]
      timestamp        <- c.downField("timestamp").as[Long]
      stateRoot        <- c.downField("stateRoot").as[String]
      adProofsRoot     <- c.downField("adProofsRoot").as[String]
      transactionsRoot <- c.downField("transactionsRoot").as[String]
      extensionHash    <- c.downField("extensionHash").as[String]
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
