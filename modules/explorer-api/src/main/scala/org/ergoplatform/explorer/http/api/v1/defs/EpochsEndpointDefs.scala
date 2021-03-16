package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.ErgoLikeContext.Height
import org.ergoplatform.explorer.db.models.EpochParameters
import org.ergoplatform.explorer.http.api.ApiErr
import sttp.tapir.{Endpoint, _}
import sttp.tapir.json.circe._

final class EpochsEndpointDefs {

  private val PathPrefix = "epochs"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getEpochInfoDef ::
    Nil

  def getEpochInfoDef: Endpoint[Unit, ApiErr, EpochParameters, Any] =
    baseEndpointDef.get
      .in(PathPrefix / "info")
      .out(jsonBody[EpochParameters])
}
