package org.ergoplatform.explorer.http.api.v1.implictis

import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.v1.models.AddressInfo
import io.circe._, io.circe.generic.semiauto._
import io.circe.syntax._
import sttp.tapir.{Schema, Validator}

object BatchAddressInfo {

  implicit val AddressKeyEncoder: KeyEncoder[Address]   = (addr: Address) => addr.unwrapped
  implicit val AddressInfoEncoder: Encoder[AddressInfo] = deriveEncoder[AddressInfo]

  implicit val EncodeBatchAddressInfo: Encoder[Map[Address, AddressInfo]] =
    (a: Map[Address, AddressInfo]) => Json.fromFields(a.map { case (a, aI) => (a.unwrapped, aI.asJson) })

  implicit val DecodeBatchAddressInfo: Decoder[Map[Address, AddressInfo]] =
    ???

  implicit val batchAddressInfoSchema: Schema[Map[Address, AddressInfo]] =
    ??? // Schema.schemaForMap[Address, AddressInfo]() TODO: Merge into version_update branch for TAPIR & dependencies upgrade
}
