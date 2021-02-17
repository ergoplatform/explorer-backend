package org.ergoplatform.explorer.indexer.models

import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.protocol.models.ApiFullBlock

final case class SlotData(apiBlock: ApiFullBlock, prevBlockInfo: Option[BlockStats])
