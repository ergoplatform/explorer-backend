package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.db.models.BlockExtension
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class FullBlockInfo(
  headerInfo: HeaderInfo,
  transactionsInfo: List[TransactionInfo],
  extension: BlockExtension,
  adProof: Option[AdProofInfo]
)

object FullBlockInfo {

  implicit val schema: Schema[FullBlockInfo] =
    implicitly[Derived[Schema[FullBlockInfo]]].value
}
