package org.ergoplatform.explorer.migration.configs

import scala.concurrent.duration.FiniteDuration

case class AssetsMigrationConfig(batchSize: Int, interval: FiniteDuration, offset: Int)
