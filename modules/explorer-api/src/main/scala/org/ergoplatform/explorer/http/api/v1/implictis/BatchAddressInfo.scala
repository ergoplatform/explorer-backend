package org.ergoplatform.explorer.http.api.v1.implictis

import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.v1.models.AddressInfo_V1
import io.circe._, io.circe.generic.semiauto._
import io.circe.syntax._
import sttp.tapir.{Schema, Validator}

object BatchAddressInfo {

  implicit val AddressKeyEncoder: KeyEncoder[Address]      = (addr: Address) => addr.unwrapped
  implicit val AddressInfoEncoder: Encoder[AddressInfo_V1] = deriveEncoder[AddressInfo_V1]

  implicit val EncodeBatchAddressInfo: Encoder[Map[Address, AddressInfo_V1]] =
    (a: Map[Address, AddressInfo_V1]) => Json.fromFields(a.map { case (a, aI) => (a.unwrapped, aI.asJson) })

  implicit val DecodeBatchAddressInfo: Decoder[Map[Address, AddressInfo_V1]] =
    ???

  implicit val batchAddressInfoSchema: Schema[Map[Address, AddressInfo_V1]] =
    ??? // Schema.schemaForMap[Address, AddressInfo]() TODO: Merge into version_update branch for TAPIR & dependencies upgrade
}
