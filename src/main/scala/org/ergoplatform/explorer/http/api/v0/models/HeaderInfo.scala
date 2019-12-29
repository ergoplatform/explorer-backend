package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{HexString, Id}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class HeaderInfo(
  id: Id,
  parentId: Id,
  version: Short,
  height: Int,
  difficulty: Long,
  adProofsRoot: HexString,
  stateRoot: HexString,
  transactionsRoot: HexString,
  timestamp: Long,
  nBits: Long,
  size: Long,
  extensionHash: HexString,
  powSolutions: PowSolutionInfo,
  votes: String
)

object HeaderInfo {

  implicit val codec: Codec[HeaderInfo] = deriveCodec

  implicit val schema: Schema[HeaderInfo] =
    implicitly[Derived[Schema[HeaderInfo]]].value
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
}
