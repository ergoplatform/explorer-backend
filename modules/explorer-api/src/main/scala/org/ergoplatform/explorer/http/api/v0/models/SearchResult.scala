package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{Address, TxId}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

// TODO ScalaDoc
final case class SearchResult(
  blocks: List[BlockInfo],
  transactionIds: List[TxId],
  addresses: List[Address]
)

object SearchResult {

  implicit def codec: Codec[SearchResult] = deriveCodec

  implicit val schema: Schema[SearchResult] =
    implicitly[Derived[Schema[SearchResult]]].value
      .modify(_.blocks)(_.description("Blocks matching search query"))
      .modify(_.transactionIds)(_.description("Ids of transaction matching search query"))
      .modify(_.addresses)(_.description("Addresses matching search query"))
}
