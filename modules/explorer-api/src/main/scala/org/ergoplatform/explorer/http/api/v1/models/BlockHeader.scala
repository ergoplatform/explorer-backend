package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{BlockId, HexString}
import org.ergoplatform.explorer.db.models.Header
import sttp.tapir.{Schema, Validator}

final case class BlockHeader(
  id: BlockId,
  parentId: BlockId,
  version: Int,
  timestamp: Long,
  height: Int,
  nBits: Long,
  votes: String,
  stateRoot: HexString,
  adProofsRoot: HexString,
  transactionsRoot: HexString,
  extensionHash: HexString,
  powSolutions: BlockPowSolutions
)

object BlockHeader {

  def apply(header: Header): BlockHeader =
    new BlockHeader(
      header.id,
      header.parentId,
      header.version,
      header.timestamp,
      header.height,
      header.nBits,
      header.votes,
      header.stateRoot,
      header.adProofsRoot,
      header.transactionsRoot,
      header.extensionHash,
      BlockPowSolutions(
        header.minerPk,
        header.w,
        header.n,
        header.d
      )
    )

  implicit val codec: Codec[BlockHeader] = deriveCodec

  implicit val schema: Schema[BlockHeader] = Schema.derived[BlockHeader]

  implicit val validator: Validator[BlockHeader] = schema.validator

}
