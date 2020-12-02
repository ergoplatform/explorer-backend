package org.ergoplatform.explorer.migration

import scala.concurrent.duration.FiniteDuration

final case class MigrationConfig(batchSize: Int, interval: FiniteDuration)
