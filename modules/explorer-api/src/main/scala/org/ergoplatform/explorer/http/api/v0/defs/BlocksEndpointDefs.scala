package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{BlockInfo, BlockSummary}
import sttp.tapir._
import sttp.tapir.json.circe._

object BlocksEndpointDefs {

  private val PathPrefix = "blocks"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getBlocksDef :: getBlockSummaryByIdDef :: Nil

  def getBlocksDef: Endpoint[Paging, ApiErr, Items[BlockInfo], Nothing] =
    baseEndpointDef
      .in(paging)
      .out(jsonBody[Items[BlockInfo]])

  def getBlockSummaryByIdDef: Endpoint[Id, ApiErr, BlockSummary, Nothing] =
    baseEndpointDef
      .in(PathPrefix / path[Id])
      .out(jsonBody[BlockSummary])
}
