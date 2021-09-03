package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.Header
import org.ergoplatform.explorer.protocol.blocks
import org.ergoplatform.explorer.{BlockId, HexString}
import scorex.util.encode.Base16
import sttp.tapir.{Schema, Validator}

final case class HeaderInfo(
  id: BlockId,
  parentId: BlockId,
  version: Short,
  height: Int,
  epoch: Int,
  difficulty: BigDecimal,
  adProofsRoot: HexString,
  stateRoot: HexString,
  transactionsRoot: HexString,
  timestamp: Long,
  nBits: Long,
  size: Int,
  extensionHash: HexString,
  powSolutions: PowSolutionInfo,
  votes: (Byte, Byte, Byte)
)

object HeaderInfo {

  implicit val codec: Codec[HeaderInfo] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit def schemaVotes: Schema[(Byte, Byte, Byte)] = Schema.derived

  implicit val schema: Schema[HeaderInfo] =
    Schema
      .derived[HeaderInfo]
      .modify(_.id)(_.description("Block/header ID"))
      .modify(_.parentId)(_.description("ID of the parental block/header"))
      .modify(_.version)(_.description("Version of the header"))
      .modify(_.height)(_.description("Block/header height"))
      .modify(_.epoch)(_.description("Block/header epoch (Epochs are enumerated from 0)"))
      .modify(_.difficulty)(_.description("Block/header difficulty"))
      .modify(_.adProofsRoot)(_.description("Hex-encoded root of the corresponding AD proofs"))
      .modify(_.stateRoot)(_.description("Hex-encoded root of the corresponding state"))
      .modify(_.transactionsRoot)(_.description("Hex-encoded root of the corresponding transactions"))
      .modify(_.timestamp)(_.description("Timestamp the block/header was created"))
      .modify(_.nBits)(_.description("Encoded required difficulty"))
      .modify(_.size)(_.description("Size of the header in bytes"))
      .modify(_.extensionHash)(_.description("Hex-encoded hash of the corresponding extension"))
      .modify(_.votes)(_.description("Block votes (3 bytes)"))

  implicit def validatorVotes: Validator[(Byte, Byte, Byte)] = schemaVotes.validator

  implicit val validator: Validator[HeaderInfo] = schema.validator

  def apply(h: Header, size: Int): HeaderInfo = {
    val powSolutions = PowSolutionInfo(h.minerPk, h.w, h.n, h.d)
    new HeaderInfo(
      h.id,
      h.parentId,
      h.version.toShort,
      h.height,
      blocks.epochOf(h.height),
      h.difficulty,
      h.adProofsRoot,
      h.stateRoot,
      h.transactionsRoot,
      h.timestamp,
      h.nBits,
      size,
      h.extensionHash,
      powSolutions,
      blocks.expandVotes(h.votes)
    )
  }
}
