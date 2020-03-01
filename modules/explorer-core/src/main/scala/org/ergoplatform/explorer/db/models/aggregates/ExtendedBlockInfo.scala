package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.db.models.BlockInfo

final case class ExtendedBlockInfo(blockInfo: BlockInfo, minerNameOpt: Option[String])
