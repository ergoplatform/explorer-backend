package org.ergoplatform.explorer.migration

import cats.effect.Sync
import org.ergoplatform.explorer.settings.DbSettings
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

final case class MigrationConfig(db: DbSettings)

object MigrationConfig {

  def load[F[_]: Sync](pathOpt: Option[String]): F[MigrationConfig] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, MigrationConfig]
}
