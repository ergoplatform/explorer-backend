package org.ergoplatform.explorer.protocol.models

import cats.syntax.either._
import io.circe.Decoder

final case class ApiDifficulty(value: BigInt)

object ApiDifficulty {

  implicit val decoder: Decoder[ApiDifficulty] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal {
          val bInt = BigInt(str)
          ApiDifficulty(bInt)
        }
        .leftMap(_ => "Difficulty")
    }
}
