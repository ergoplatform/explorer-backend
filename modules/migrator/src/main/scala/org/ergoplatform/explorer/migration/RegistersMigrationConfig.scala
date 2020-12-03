package org.ergoplatform.explorer.migration

import scala.concurrent.duration.FiniteDuration

final case class RegistersMigrationConfig(batchSize: Int, interval: FiniteDuration)
