package org.ergoplatform.explorer.migration.configs

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.settings.{DbSettings, SettingCompanion}

final case class MigrationConfig(
  db: DbSettings,
  processing: ProcessingConfig,
  networkPrefix: String Refined ValidByte
) {

  val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)
}

object MigrationConfig extends SettingCompanion[MigrationConfig]
