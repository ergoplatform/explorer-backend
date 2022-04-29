package org.ergoplatform.explorer.http.api.v1.implictis

import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.http.api.v1.models.AddressInfo
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.ergoplatform.explorer.http.api.v1.implictis.schemaForMapKV.schemaForMap
import sttp.tapir.{Schema, Validator}
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

object BatchAddressInfo {

  implicit val AddressKeyEncoder: KeyEncoder[Address]   = (addr: Address) => addr.unwrapped
  implicit val AddressInfoEncoder: Encoder[AddressInfo] = deriveEncoder[AddressInfo]

  implicit val EncodeBatchAddressInfo: Encoder[Map[Address, AddressInfo]] =
    (a: Map[Address, AddressInfo]) => Json.fromFields(a.map { case (a, aI) => (a.unwrapped, aI.asJson) })

  def failWithMsg(msg: String): DecodingFailure = DecodingFailure(msg, List.empty)

  implicit val DecodeBatchAddressInfo: Decoder[Map[Address, AddressInfo]] = { c: HCursor =>
    for {
      jsonObject  <- c.value.asObject.toRight(failWithMsg(s"${c.value} is not an object"))
      listOfTuple <- jsonObject.keys.map(s => c.downField(s).as[AddressInfo].map(x => (x.address, x))).toList.sequence
    } yield listOfTuple.toMap
  }

  implicit val schema: Schema[Map[Address, AddressInfo]] =
    schemaForMap[Address, AddressInfo]

  implicit val validator: Validator[Map[Address, AddressInfo]] = schema.validator
}
