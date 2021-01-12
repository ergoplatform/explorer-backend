package org.ergoplatform.explorer.migration.configs

import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.settings.DbSettings
import org.ergoplatform.explorer.settings.pureConfigInstances._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

final case class MigrationConfig(
  db: DbSettings,
  migrationId: String,
  offset: Int,
  networkPrefix: String Refined ValidByte
) {

  val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)
}

object MigrationConfig {

  def load[F[_]: Sync](pathOpt: Option[String]): F[MigrationConfig] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, MigrationConfig]
}
