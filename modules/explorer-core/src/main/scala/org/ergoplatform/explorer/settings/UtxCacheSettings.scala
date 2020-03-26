package org.ergoplatform.explorer.settings

import scala.concurrent.duration.FiniteDuration

final case class UtxCacheSettings(transactionTtl: FiniteDuration)
