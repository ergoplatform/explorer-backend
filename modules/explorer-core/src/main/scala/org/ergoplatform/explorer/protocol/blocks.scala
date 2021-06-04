package org.ergoplatform.explorer.protocol

import scorex.util.encode.Base16

object blocks {

  @inline def epochOf(height: Int): Int = height / constants.EpochLength

  @inline def expandVotes(votesHex: String): (Byte, Byte, Byte) = {
    val defaultVotes = (0: Byte, 0: Byte, 0: Byte)
    val paramsQty    = 3
    Base16
      .decode(votesHex)
      .map {
        case votes if votes.length == paramsQty => (votes(0): Byte, votes(1): Byte, votes(2): Byte)
        case _                                  => defaultVotes
      }
      .getOrElse(defaultVotes)
  }
}
