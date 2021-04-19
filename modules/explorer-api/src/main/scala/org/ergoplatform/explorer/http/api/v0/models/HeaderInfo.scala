package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.Header
import org.ergoplatform.explorer.{HexString, Id}
import scorex.util.encode.Base16
import sttp.tapir.{Schema, Validator}
import sttp.tapir.generic.Derived

final case class HeaderInfo(
  id: Id,
  parentId: Id,
  version: Short,
  height: Int,
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
      h.difficulty,
      h.adProofsRoot,
      h.stateRoot,
      h.transactionsRoot,
      h.timestamp,
      h.nBits,
      size,
      h.extensionHash,
      powSolutions,
      expandVotes(h.votes)
    )
  }

  private def expandVotes(votesHex: String) = {
    val defaultVotes = (0: Byte, 0: Byte, 0: Byte)
    val paramsQty    = 3
    Base16
      .decode(votesHex)
      .map {
        case votes if votes.length == paramsQty => (votes(0): Byte, votes(1): Byte, votes(2): Byte)
        case _                                  => defaultVotes
      }
      .getOrElse(defaultVotes)
  }
}
