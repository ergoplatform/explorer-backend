package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Json
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.Output
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedAsset, ExtendedOutput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import org.ergoplatform.explorer.protocol.models.ExpandedRegister
import sttp.tapir.{Schema, SchemaType, Validator}

@derive(encoder, decoder)
final case class OutputInfo(
  boxId: BoxId,
  transactionId: TxId,
  blockId: BlockId,
  value: Long,
  index: Int,
  globalIndex: Long,
  creationHeight: Int,
  settlementHeight: Int,
  ergoTree: HexString,
  ergoTreeConstants: String,
  ergoTreeScript: String,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json,
  spentTransactionId: Option[TxId],
  mainChain: Boolean
)

object OutputInfo {

  implicit val schema: Schema[OutputInfo] =
    Schema
      .derived[OutputInfo]
      .modify(_.boxId)(_.description("Id of the box"))
      .modify(_.transactionId)(_.description("Id of the transaction that created the box"))
      .modify(_.blockId)(_.description("Id of the block a box included in"))
      .modify(_.value)(_.description("Value of the box in nanoERG"))
      .modify(_.index)(_.description("Index of the output in a transaction"))
      .modify(_.globalIndex)(_.description("Global index of the output in the blockchain"))
      .modify(_.creationHeight)(_.description("Height at which the box was created"))
      .modify(_.settlementHeight)(_.description("Height at which the box got fixed in blockchain"))
      .modify(_.ergoTree)(_.description("Serialized ergo tree"))
      .modify(_.address)(_.description("An address derived from ergo tree"))
      .modify(_.spentTransactionId)(_.description("Id of the transaction this output was spent by"))

  implicit val validator: Validator[OutputInfo] = schema.validator

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        Schema.derived[ExpandedRegister]
          .modify(_.serializedValue)(_.description("Hex-encoded serialized register value"))
          .modify(_.sigmaType)(_.description("Type of the register value"))
          .modify(_.renderedValue)(_.description("Human-readable rendered register value"))
          .as[Json]
      )(_ => Map.empty)
    )

  def apply(
    o: ExtendedOutput,
    assets: List[ExtendedAsset]
  ): OutputInfo = {
    val (ergoTreeConstants, ergoTreeScript) = PrettyErgoTree.fromHexString(o.output.ergoTree).fold(
      _ => ("", ""),
      tree => (tree.constants, tree.script)
    )
    OutputInfo(
      o.output.boxId,
      o.output.txId,
      o.output.headerId,
      o.output.value,
      o.output.index,
      o.output.globalIndex,
      o.output.creationHeight,
      o.output.settlementHeight,
      o.output.ergoTree,
      ergoTreeConstants,
      ergoTreeScript,
      o.output.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      o.output.additionalRegisters,
      o.spentByOpt,
      o.output.mainChain
    )
  }

  def unspent(
    o: Output,
    assets: List[ExtendedAsset]
  ): OutputInfo = {
    val (ergoTreeConstants, ergoTreeScript) = PrettyErgoTree.fromHexString(o.ergoTree).fold(
      _ => ("", ""),
      tree => (tree.constants, tree.script)
    )
    OutputInfo(
      o.boxId,
      o.txId,
      o.headerId,
      o.value,
      o.index,
      o.globalIndex,
      o.creationHeight,
      o.settlementHeight,
      o.ergoTree,
      ergoTreeConstants,
      ergoTreeScript,
      o.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      o.additionalRegisters,
      None,
      o.mainChain
    )
  }
}
