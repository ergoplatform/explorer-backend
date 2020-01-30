package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import sttp.tapir._
import sttp.tapir.json.circe._

object DexEndpointsDefs {

  private val PathPrefix = "dex"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getUnspentSellOrdersDef :: getUnspentBuyOrdersDef :: Nil

  def getUnspentSellOrdersDef: Endpoint[TokenId, ApiErr, List[OutputInfo], Nothing] =
    baseEndpointDef
      .in(PathPrefix / "tokens" / path[TokenId] / "unspentSellOrders")
      .out(jsonBody[List[OutputInfo]])

  def getUnspentBuyOrdersDef: Endpoint[TokenId, ApiErr, List[OutputInfo], Nothing] =
    baseEndpointDef
      .in(PathPrefix / "tokens" / path[TokenId] / "unspentBuyOrders")
      .out(jsonBody[List[OutputInfo]])
}
