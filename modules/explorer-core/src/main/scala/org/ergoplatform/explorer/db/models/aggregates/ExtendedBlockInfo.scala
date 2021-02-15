package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.db.models.BlockStats

final case class ExtendedBlockInfo(blockInfo: BlockStats, minerNameOpt: Option[String])
