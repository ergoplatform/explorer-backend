package org.ergoplatform.explorer.http.api.v0.defs

import cats.data.NonEmptyMap
import cats.instances.string._
import cats.syntax.option._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{BlockInfo, BlockSummary}
import sttp.tapir._
import sttp.tapir.json.circe._

object BlocksEndpointDefs {

  private val PathPrefix = "blocks"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getBlocksDef :: getBlockSummaryByIdDef :: Nil

  def getBlocksDef: Endpoint[(Paging, Sorting), ApiErr, Items[BlockInfo], Nothing] =
    baseEndpointDef
      .in(paging)
      .in(sorting(allowedSortingFields, defaultField = "height".some))
      .in(PathPrefix)
      .out(jsonBody[Items[BlockInfo]])

  def getBlockSummaryByIdDef: Endpoint[Id, ApiErr, BlockSummary, Nothing] =
    baseEndpointDef
      .in(PathPrefix / path[Id])
      .out(jsonBody[BlockSummary])

  val allowedSortingFields: NonEmptyMap[String, String] =
    NonEmptyMap.of(
      "height"            -> "height",
      "timestamp"         -> "timestamp",
      "transactionscount" -> "txs_count",
      "size"              -> "block_size",
      "miner"             -> "miner_name",
      "difficulty"        -> "difficulty",
      "minerreward"       -> "miner_reward"
    )
}
