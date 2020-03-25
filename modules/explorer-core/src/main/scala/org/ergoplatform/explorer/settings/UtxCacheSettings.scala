package org.ergoplatform.explorer.settings

import scala.concurrent.duration.FiniteDuration

// TODO ScalaDoc
final case class UtxCacheSettings(transactionTtl: FiniteDuration)
