package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.{InclusionHeightRange, Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.{AddressInfo, Balance, TotalBalance, TransactionInfo}
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.tapir.json.circe._
import sttp.tapir.{path, _}

class AddressesEndpointDefs(settings: RequestsSettings) {

  private val PathPrefix = "addresses"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getTxsByAddressDef :: getConfirmedBalanceDef :: getTotalBalanceDef :: Nil

  def getTxsByAddressDef
    : Endpoint[(Address, Paging, Boolean, Option[InclusionHeightRange]), ApiErr, Items[TransactionInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[Address] / "transactions")
      .in(paging(settings.maxEntitiesPerRequest))
      .in(
        query[Boolean]("concise")
          .default(false)
          .description("Display only address inputs/outputs in transaction")
      )
      .in(inclusionHeight)
      .out(jsonBody[Items[TransactionInfo]])

  def getConfirmedBalanceDef: Endpoint[(Address, Int), ApiErr, Balance, Any] =
    baseEndpointDef
      .in(PathPrefix / path[Address] / "balance" / "confirmed")
      .in(confirmations)
      .out(jsonBody[Balance])

  def getTotalBalanceDef: Endpoint[Address, ApiErr, TotalBalance, Any] =
    baseEndpointDef
      .in(PathPrefix / path[Address] / "balance" / "total")
      .out(jsonBody[TotalBalance])

  def getBatchAddressInfo: Endpoint[List[Address], ApiErr, Map[Address, AddressInfo], Any] =
    baseEndpointDef
      .in(PathPrefix / "batchInfo")
      .in(jsonBody[List[Address]])
      .out(jsonBody[Map[Address, AddressInfo]])
}
