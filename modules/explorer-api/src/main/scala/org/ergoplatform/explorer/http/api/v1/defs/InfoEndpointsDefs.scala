package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v1.models.NetworkInfo
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.jsonBody

final class InfoEndpointsDefs {

  def endpoints: List[Endpoint[_, _, _, _]] =
    getNetworkInfo :: Nil

  def getNetworkInfo: Endpoint[Unit, ApiErr, NetworkInfo, Any] =
    baseEndpointDef.get
      .in("info")
      .out(jsonBody[NetworkInfo])
}
