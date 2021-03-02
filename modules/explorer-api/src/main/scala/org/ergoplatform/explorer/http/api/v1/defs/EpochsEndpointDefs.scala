package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.ErgoLikeContext.Height
import org.ergoplatform.explorer.db.models.EpochParameters
import org.ergoplatform.explorer.http.api.ApiErr
import sttp.tapir.{Endpoint, _}
import sttp.tapir.json.circe._

final class EpochsEndpointDefs[F[_]] {

  private val PathPrefix = "epochs"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getEpochInfoByHeightDef ::
    getEpochInfoByIdDef ::
    Nil

  def getEpochInfoByHeightDef: Endpoint[Height, ApiErr, EpochParameters, Any] =
    baseEndpointDef.get
      .in(PathPrefix / "info" / "height" / path[Height])
      .out(jsonBody[EpochParameters])

  def getEpochInfoByIdDef: Endpoint[Int, ApiErr, EpochParameters, Any] =
    baseEndpointDef.get
      .in(PathPrefix / "info" / "epoch" / path[Int])
      .out(jsonBody[EpochParameters])
}
