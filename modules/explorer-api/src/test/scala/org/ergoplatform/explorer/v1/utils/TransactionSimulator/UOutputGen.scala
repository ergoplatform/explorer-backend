package org.ergoplatform.explorer.v1.utils.TransactionSimulator

import io.circe.Json
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.http.api.v1.models.UOutputInfo
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}

import java.util.UUID.randomUUID

object UOutputGen {

  def apply(
    value: Long,
    ergoTree: HexString,
    address: Address,
    assets: List[AssetInstanceInfo]
  ): UOutputInfo = UOutputInfo(
    boxId               = BoxId(randomUUID().toString),
    transactionId       = TxId(randomUUID().toString),
    value               = value,
    index               = 0,
    creationHeight      = 1,
    ergoTree            = ergoTree,
    address             = address,
    assets              = assets,
    additionalRegisters = Json.Null,
    spentTransactionId  = None
  )
}
