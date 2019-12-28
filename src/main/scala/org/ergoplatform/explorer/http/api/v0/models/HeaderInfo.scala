package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.{HexString, Id}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class HeaderInfo(
  id: Id,
  parentId: String,
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
}
