package org.ergoplatform.explorer.persistence.models

import org.ergoplatform.explorer.{AssetId, BoxId}

final case class Asset(id: AssetId, boxId: BoxId, amount: Long)
