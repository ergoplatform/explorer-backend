package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{
  DexBuyOrderInfo,
  DexSellOrderInfo,
  OutputInfo
}
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.commonDirectives._
import sttp.tapir._
import sttp.tapir.json.circe._

object DexEndpointsDefs {

  private val PathPrefix = "dex"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getUnspentSellOrdersDef :: getUnspentBuyOrdersDef :: Nil

  def getUnspentSellOrdersDef
    : Endpoint[(Paging, TokenId), ApiErr, List[DexSellOrderInfo], Nothing] =
    baseEndpointDef
      .in(paging)
      .in(PathPrefix / "tokens" / path[TokenId] / "unspentSellOrders")
      .out(jsonBody[List[DexSellOrderInfo]])

  def getUnspentBuyOrdersDef
    : Endpoint[(Paging, TokenId), ApiErr, List[DexBuyOrderInfo], Nothing] =
    baseEndpointDef
      .in(paging)
      .in(PathPrefix / "tokens" / path[TokenId] / "unspentBuyOrders")
      .out(jsonBody[List[DexBuyOrderInfo]])
}
