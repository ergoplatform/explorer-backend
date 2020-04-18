package org.ergoplatform.dex.streaming

import io.circe.Decoder
import org.ergoplatform.explorer.db.models.Output

object schemaCodecs {

  implicit def outputDecoder: Decoder[Output] = ???
}
