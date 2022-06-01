package org.ergoplatform.explorer.db.models.aggregates

import org.ergoplatform.explorer.BlockId

case class BlockSize(headerId: BlockId, size: Int)
