package org.ergoplatform.explorer.migration.configs

import scala.concurrent.duration.FiniteDuration

final case class RegistersMigrationConfig(batchSize: Int, interval: FiniteDuration, offset: Int)
