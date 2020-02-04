package org.ergoplatform.explorer.db

import java.sql.Types
import io.circe.Json
import io.circe.parser.parse

object quillCodecs {

  import org.ergoplatform.explorer.db.doobieInstances._
  import org.ergoplatform.explorer.db.models.schema.ctx._

  implicit val jsonEncoder: Encoder[Json] =
    encoder(Types.VARCHAR, (i, v, r) => r.setString(i, v.noSpaces))

  implicit val jsonDecoder: Decoder[Json] =
    decoder((i, r) => parse(r.getString(i)).right.getOrElse(Json.Null))

}
