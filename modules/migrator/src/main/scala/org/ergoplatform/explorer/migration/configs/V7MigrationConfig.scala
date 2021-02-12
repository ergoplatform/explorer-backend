package org.ergoplatform.explorer.migration.configs

import scala.concurrent.duration.FiniteDuration

final case class V7MigrationConfig(batchSize: Int, interval: FiniteDuration, offset: Int, parallelism: Int)
