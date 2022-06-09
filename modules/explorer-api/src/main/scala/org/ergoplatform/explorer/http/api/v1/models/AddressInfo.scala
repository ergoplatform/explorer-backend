package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json, KeyEncoder}
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.ergoplatform.explorer.{Address, HexString}
import org.ergoplatform.explorer.http.api.v1.implictis.schemaForMapKV.schemaForMap
import sttp.tapir.{Schema, Validator}
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import org.ergoplatform.explorer.db.models.aggregates.AggregatedAsset

@derive(encoder, decoder)
final case class AddressInfo(
  address: Address,
  hasUnconfirmedTxs: Boolean,
  used: Boolean,
  confirmedBalance: Balance
)

object AddressInfo {

  implicit val schema: Schema[AddressInfo] =
    Schema
      .derived[AddressInfo]
      .modify(_.address)(_.description("Address"))
      .modify(_.hasUnconfirmedTxs)(_.description("BOOLEAN unconfirmed transactions"))
      .modify(_.used)(_.description("BOOLEAN"))
      .modify(_.confirmedBalance)(_.description("Confirmed balance in address"))

  implicit val validator: Validator[AddressInfo] = schema.validator

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

  implicit val schemaBatchAddressInfo: Schema[Map[Address, AddressInfo]] =
    schemaForMap[Address, AddressInfo]

  implicit val validatorBatchAddressInfo: Validator[Map[Address, AddressInfo]] = schemaBatchAddressInfo.validator

  def empty(address: Address): AddressInfo =
    AddressInfo(address, hasUnconfirmedTxs = false, used = false, Balance.empty)

  def makeInfo(
    batch: List[(Address, HexString)],
    batchUnspentSums: Map[HexString, Long],
    batchAssets: Map[HexString, List[AggregatedAsset]],
    batchUsedState: Map[HexString, Boolean],
    batchUTxState: Map[HexString, Boolean]
  ): List[(Address, AddressInfo)] =
    batch.map { case (addr, hex) =>
      val sums             = batchUnspentSums.get(hex)
      val assets           = batchAssets.get(hex)
      val hasBeenUsed      = batchUsedState.get(hex)
      val hasUnconfirmedTx = batchUTxState.get(hex)

      val addrInfo =
        (for {
          nanoErg <- sums
          aA      <- assets
          hUtx    <- hasUnconfirmedTx
          hbu     <- hasBeenUsed
        } yield AddressInfo(addr, hUtx, hbu, Balance(nanoErg, aA.map(TokenAmount(_)))))
          .getOrElse(AddressInfo.empty(addr))

      (addr, addrInfo)
    }

}
