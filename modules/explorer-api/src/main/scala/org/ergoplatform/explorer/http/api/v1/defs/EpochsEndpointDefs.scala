package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v1.models.EpochInfo
import sttp.tapir.{Endpoint, _}
import sttp.tapir.json.circe._

final class EpochsEndpointDefs {

  private val PathPrefix = "epochs"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getEpochInfoDef :: Nil

  def getEpochInfoDef: Endpoint[Unit, ApiErr, EpochInfo, Any] =
    baseEndpointDef.get
      .in(PathPrefix / "params")
      .out(jsonBody[EpochInfo])
}
