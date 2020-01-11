package org.ergoplatform.explorer

import cats.instances.try_._

import scala.util.Try

object testConstants {

  val MainNetMinerPk: HexString = HexString
    .fromString[Try](
      "0377d854c54490abc6c565d8e548d5fc92a6a6c2f4415ed96f0c340ece92e1ed2f"
    )
    .get
}
