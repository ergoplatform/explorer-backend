package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.models.StatsSummary
import sttp.tapir.Endpoint
import sttp.tapir._
import sttp.tapir.json.circe._

object StatsEndpointDefs {

  private val PathPrefix = "stats"

  def endpoints: List[Endpoint[_, _, _, _]] = getCurrentStatsDef :: Nil

  def getCurrentStatsDef: Endpoint[Unit, ApiErr, StatsSummary, Any] =
    baseEndpointDef.get.in(PathPrefix).out(jsonBody[StatsSummary])
}
