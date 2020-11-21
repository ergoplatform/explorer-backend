package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.{Address, BoxId, HexString}
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.models.OutputInfo
import sttp.tapir._
import sttp.tapir.json.circe._

object BoxesEndpointDefs {

  private val PathPrefix = "transactions" / "boxes"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getOutputByIdDef :: getOutputsByErgoTreeDef :: getUnspentOutputsByErgoTreeDef ::
    getOutputsByAddressDef :: getUnspentOutputsByAddressDef :: Nil

  def getOutputByIdDef: Endpoint[BoxId, ApiErr, OutputInfo, Any] =
    baseEndpointDef.get.in(PathPrefix / path[BoxId]).out(jsonBody[OutputInfo])

  def getOutputsByErgoTreeDef: Endpoint[HexString, ApiErr, List[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byErgoTree" / path[HexString])
      .out(jsonBody[List[OutputInfo]])

  def getUnspentOutputsByErgoTreeDef: Endpoint[HexString, ApiErr, List[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byErgoTree" / "unspent" / path[HexString])
      .out(jsonBody[List[OutputInfo]])

  def getOutputsByAddressDef: Endpoint[Address, ApiErr, List[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byAddress" / path[Address])
      .out(jsonBody[List[OutputInfo]])

  def getUnspentOutputsByAddressDef: Endpoint[Address, ApiErr, List[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byAddress" / "unspent" / path[Address])
      .out(jsonBody[List[OutputInfo]])
}
