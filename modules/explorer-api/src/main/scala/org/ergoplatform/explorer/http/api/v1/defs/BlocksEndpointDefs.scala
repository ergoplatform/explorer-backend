package org.ergoplatform.explorer.http.api.v1.defs

import cats.data.NonEmptyMap
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives.{paging, sorting}
import org.ergoplatform.explorer.http.api.models.{Items, Paging, Sorting}
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.tapir.{Endpoint, path}
import sttp.tapir.json.circe.jsonBody
import cats.syntax.option._
import org.ergoplatform.explorer.http.api.v1.models.{BlockInfo, BlockSummary}
import sttp.tapir._
import sttp.tapir.json.circe._

class BlocksEndpointDefs[F[_]](settings: RequestsSettings) {

  private val PathPrefix = "blocks"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getBlocksDef :: getBlockSummaryByIdDef :: Nil

  def getBlocksDef: Endpoint[(Paging, Sorting), ApiErr, Items[BlockInfo], Any] =
    baseEndpointDef.get
      .in(paging(settings.maxEntitiesPerRequest))
      .in(sorting(allowedSortingFields, defaultFieldOpt = "height".some))
      .in(PathPrefix)
      .out(jsonBody[Items[BlockInfo]])

  def getBlockSummaryByIdDef: Endpoint[Id, ApiErr, BlockSummary, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[Id])
      .out(jsonBody[BlockSummary])

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
