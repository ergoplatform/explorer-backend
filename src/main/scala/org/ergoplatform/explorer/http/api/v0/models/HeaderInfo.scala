package org.ergoplatform.explorer.http.api.v0.models

import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class HeaderInfo(
  id: String,
  parentId: String,
  version: Short,
  height: Long,
  difficulty: Long,
  adProofsRoot: String,
  stateRoot: String,
  transactionsRoot: String,
  timestamp: Long,
  nBits: Long,
  size: Long,
  extensionHash: String,
  powSolutions: PowSolutionInfo,
  votes: String
)

object HeaderInfo {

  implicit val schema: Schema[HeaderInfo] =
    implicitly[Derived[Schema[HeaderInfo]]].value
}
