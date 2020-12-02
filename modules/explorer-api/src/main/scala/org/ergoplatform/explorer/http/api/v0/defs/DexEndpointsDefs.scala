package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.v0.models.{DexBuyOrderInfo, DexSellOrderInfo}
import sttp.tapir._
import sttp.tapir.json.circe._

object DexEndpointsDefs {

  private val PathPrefix = "dex"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getUnspentSellOrdersDef :: getUnspentBuyOrdersDef :: Nil

  def getUnspentSellOrdersDef
    : Endpoint[(TokenId, Paging), ApiErr, List[DexSellOrderInfo], Any] =
    baseEndpointDef.get
      .description("DEX sell orders for a given token id")
      .in(PathPrefix / "tokens" / path[TokenId] / "unspentSellOrders")
      .in(paging)
      .out(jsonBody[List[DexSellOrderInfo]].description("Sell order for DEX, where ask is in tokenPrice, DEX fee is in outputInfo.value and token is in outputInfo.assets(0)"))

  def getUnspentBuyOrdersDef
    : Endpoint[(TokenId, Paging), ApiErr, List[DexBuyOrderInfo], Any] =
    baseEndpointDef.get
      .description("DEX buy orders for a given token id")
      .in(PathPrefix / "tokens" / path[TokenId] / "unspentBuyOrders")
      .in(paging)
      .out(jsonBody[List[DexBuyOrderInfo]].description("Buy order for DEX, where bid + DEX fee are in outputInfo.value and tokenId and tokenAmount are parsed from buyer's contract"))
}
