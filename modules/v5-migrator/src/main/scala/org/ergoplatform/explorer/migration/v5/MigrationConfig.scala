package org.ergoplatform.explorer.migration.v5

import scala.concurrent.duration.FiniteDuration

final case class MigrationConfig(batchSize: Int, interval: FiniteDuration)
