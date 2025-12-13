package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedInput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}
import sttp.tapir.{Schema, Validator}
import sttp.tapir.generic.Derived

final case class InputInfo(
  id: BoxId,
  value: Option[Long],
  index: Int,
  spendingProof: Option[HexString],
  transactionId: TxId,
  outputTransactionId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address],
  assets: List[AssetInstanceInfo]
)

object InputInfo {

  implicit val codec: Codec[InputInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[InputInfo] =
    Schema
      .derived[InputInfo]
      .modify(_.id)(_.description("ID of the corresponding box"))
      .modify(_.spendingProof)(_.description("Hex-encoded serialized sigma proof"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.transactionId)(_.description("ID of the transaction this input was used in"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))
      .modify(_.assets)(_.description("Assets (tokens) in the corresponding box"))

  implicit val validator: Validator[InputInfo] = schema.validator

  def apply(i: ExtendedInput, assets: List[ExtendedAsset]): InputInfo =
    InputInfo(
      i.input.boxId,
      i.value,
      i.input.index,
      i.input.proofBytes,
      i.input.txId,
      i.outputTxId,
      i.outputIndex,
      i.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_))
    )

  def batch(ins: List[ExtendedInput], assets: List[ExtendedAsset]): List[InputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    ins
      .sortBy(_.input.index)
      .map(in => InputInfo(in, groupedAssets.get(in.input.boxId).toList.flatten))
  }
}
