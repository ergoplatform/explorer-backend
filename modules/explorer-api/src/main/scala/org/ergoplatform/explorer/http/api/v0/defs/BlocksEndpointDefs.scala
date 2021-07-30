package org.ergoplatform.explorer.http.api.v0.defs

import cats.data.NonEmptyMap
import cats.instances.string._
import cats.syntax.option._
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{BlockInfo, BlockSummary}
import sttp.tapir._
import sttp.tapir.json.circe._

object BlocksEndpointDefs {

  private val PathPrefix = "blocks"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getBlocksDef :: getBlockSummaryByIdDef :: getBlockIdsAtHeightDef :: Nil

  def getBlocksDef: Endpoint[(Paging, Sorting), ApiErr, Items[BlockInfo], Any] =
    baseEndpointDef.get
      .in(paging)
      .in(sorting(allowedSortingFields, defaultFieldOpt = "height".some))
      .in(PathPrefix)
      .out(jsonBody[Items[BlockInfo]])

  def getBlockSummaryByIdDef: Endpoint[BlockId, ApiErr, BlockSummary, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[BlockId])
      .out(jsonBody[BlockSummary])

  def getBlockIdsAtHeightDef: Endpoint[Int, ApiErr, List[BlockId], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "at" / path[Int])
      .out(jsonBody[List[BlockId]])

  val allowedSortingFields: NonEmptyMap[String, String] =
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
}
