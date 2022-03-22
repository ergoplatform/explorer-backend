package org.ergoplatform.explorer.http.api.v1.defs

import cats.data.NonEmptyMap
import cats.syntax.option._
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives.{limit, minGlobalIndex, paging, sorting}
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.http.api.v0.models.BlockSummary
import org.ergoplatform.explorer.http.api.v1.models.{BlockHeader, BlockInfo}
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._
import sttp.tapir.json.circe._

class BlocksEndpointDefs[F[_]](settings: RequestsSettings) {

  private val PathPrefix = "blocks"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getBlocksDef :: getBlockSummaryByIdDef :: getBlockHeadersDef :: streamBlocksDef :: Nil

  def getBlocksDef: Endpoint[(Paging, Sorting), ApiErr, Items[BlockInfo], Any] =
    baseEndpointDef.get
      .in(paging(settings.maxEntitiesPerRequest))
      .in(sorting(allowedBlockSortingFields, defaultFieldOpt = "height".some))
      .in(PathPrefix)
      .out(jsonBody[Items[BlockInfo]])

  def streamBlocksDef: Endpoint[(Long, Int), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "byGlobalIndex" / "stream")
      .in(minGlobalIndex)
      .in(limit(settings.maxEntitiesPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[BlockInfo]], CodecFormat.Json(), None))
      .description("Get a stream of blocks ordered by global index (height)")

  def getBlockSummaryByIdDef: Endpoint[BlockId, ApiErr, BlockSummary, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[BlockId])
      .out(jsonBody[BlockSummary])

  def getBlockHeadersDef: Endpoint[(Paging, Sorting), ApiErr, Items[BlockHeader], Any] =
    baseEndpointDef.get
      .in(paging(settings.maxEntitiesPerRequest))
      .in(sorting(allowedHeaderSortingFields, defaultFieldOpt = "height".some))
      .in(PathPrefix / "headers")
      .out(jsonBody[Items[BlockHeader]])

  val allowedBlockSortingFields: NonEmptyMap[String, String] =
    NonEmptyMap.of(
      "height"            -> "height",
      "timestamp"         -> "timestamp",
      "transactionscount" -> "txs_count",
      "size"              -> "block_size",
      "difficulty"        -> "difficulty",
      "minerreward"       -> "miner_reward",
      "blockfee"          -> "block_fee",
      "blockcoins"        -> "block_coins"
    )

  val allowedHeaderSortingFields: NonEmptyMap[String, String] =
    NonEmptyMap.of(
      "timestamp" -> "timestamp",
      "height"    -> "height"
    )
}
