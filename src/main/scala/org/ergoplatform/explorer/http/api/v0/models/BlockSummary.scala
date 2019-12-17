package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.{Address, Id}
import sttp.tapir.{CodecForOptional, CodecFormat, Schema}

final case class BlockSummary(id: Id, address: Address)

object BlockSummary {

  import sttp.tapir.json.circe._
  import io.circe.generic.auto._

  implicit val codec: CodecForOptional[BlockSummary, CodecFormat.Json, _] =
    implicitly[CodecForOptional[BlockSummary, CodecFormat.Json, _]]

  implicit val schema: Schema[BlockSummary] =
    implicitly[Schema[BlockSummary]]
      .modify(_.id)(_.description("Id of the block"))
}
