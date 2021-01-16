package org.ergoplatform.explorer.grabber.models

import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.protocol.models.ApiFullBlock

final case class SlotData(apiBlock: ApiFullBlock, prevBlockInfo: Option[BlockInfo])
