package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.{Address, TokenId}
import org.ergoplatform.explorer.Err.ApiErr
import org.ergoplatform.explorer.http.api.models.Paging
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.v0.models.{AddressInfo, TransactionInfo}
import sttp.tapir._
import sttp.tapir.json.circe._

object AddressesEndpointDefs {

  private val PathPrefix = "addresses"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getAddressDef :: getTxsByAddressDef :: getAssetHoldersDef :: Nil

  def getAddressDef: Endpoint[Address, ApiErr, AddressInfo, Nothing] =
    baseEndpointDef
      .in(PathPrefix / path[Address])
      .out(jsonBody[AddressInfo])

  def getTxsByAddressDef: Endpoint[(Address, Paging), ApiErr, List[TransactionInfo], Nothing] =
    baseEndpointDef
      .in(PathPrefix / path[Address] / "transactions")
      .in(paging)
      .out(jsonBody[List[TransactionInfo]])

  def getAssetHoldersDef: Endpoint[(TokenId, Paging), ApiErr, List[Address], Nothing] =
    baseEndpointDef
      .in(PathPrefix / "assetHolders" / path[TokenId])
      .in(paging)
      .out(jsonBody[List[Address]])
}
