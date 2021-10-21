package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v1.models.{NetworkState, NetworkStats}
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.jsonBody

final class StatsEndpointsDefs {

  def endpoints: List[Endpoint[_, _, _, _]] =
    getNetworkInfo :: getNetworkState :: getNetworkStats :: Nil

  def getNetworkInfo: Endpoint[Unit, ApiErr, NetworkState, Any] =
    baseEndpointDef.get
      .in("info")
      .out(jsonBody[NetworkState])
      .deprecated()

  def getNetworkState: Endpoint[Unit, ApiErr, NetworkState, Any] =
    baseEndpointDef.get
      .in("networkState")
      .out(jsonBody[NetworkState])

  def getNetworkStats: Endpoint[Unit, ApiErr, NetworkStats, Any] =
    baseEndpointDef.get
      .in("networkStats")
      .out(jsonBody[NetworkStats])
}
