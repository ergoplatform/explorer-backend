package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.models.BlockChainInfo
import sttp.tapir.Endpoint

import sttp.tapir._
import sttp.tapir.json.circe._

object InfoEndpointDefs {

  private val PathPrefix = "info"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getBlockChainInfoDef :: getCurrentSupplyDef :: Nil

  def getBlockChainInfoDef: Endpoint[Unit, ApiErr, BlockChainInfo, Any] =
    baseEndpointDef.get.in(PathPrefix).out(jsonBody[BlockChainInfo])

  def getCurrentSupplyDef: Endpoint[Unit, ApiErr, String, Any] =
    baseEndpointDef.get.in(PathPrefix / "supply").out(plainBody[String])
}
