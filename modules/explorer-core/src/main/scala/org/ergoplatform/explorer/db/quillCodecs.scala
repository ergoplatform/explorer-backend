package org.ergoplatform.explorer.db

import java.sql.Types
import io.circe.Json

object quillCodecs {

  import org.ergoplatform.explorer.db.models.schema.ctx._

  implicit val jsonEncoder: Encoder[Json] =
    encoder(Types.VARCHAR, (i, v, r) => r.setString(i, v.toString))

  implicit val jsonDecoder: Decoder[Json] =
    decoder((i, r) => Json.fromString(r.getString(i)))

}
