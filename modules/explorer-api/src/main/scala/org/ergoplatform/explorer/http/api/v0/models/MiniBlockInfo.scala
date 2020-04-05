package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Id
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class MiniBlockInfo(id: Id, height: Int)

object MiniBlockInfo {

  implicit val codec: Codec[MiniBlockInfo] = deriveCodec

  implicit val schema: Schema[MiniBlockInfo] =
    implicitly[Derived[Schema[MiniBlockInfo]]].value
      .modify(_.id)(_.description("Block ID"))
      .modify(_.height)(_.description("Block height"))
}
