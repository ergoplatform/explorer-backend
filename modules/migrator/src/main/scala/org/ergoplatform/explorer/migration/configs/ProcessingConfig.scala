package org.ergoplatform.explorer.migration.configs

import scala.concurrent.duration.FiniteDuration

final case class ProcessingConfig(batchSize: Int, interval: FiniteDuration, offset: Int, updateSchema: Boolean)
