package org.ergoplatform.explorer.settings

import cats.effect.Sync
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import org.ergoplatform.explorer.settings.pureConfigInstances._

final case class ApiAppSettings(
  http: HttpSettings,
  db: DbSettings,
  protocol: ProtocolSettings,
  utxCache: UtxCacheSettings,
  redis: RedisSettings,
  service: ServiceSettings
)

object ApiAppSettings {

  def load[F[_]: Sync](pathOpt: Option[String]): F[ApiAppSettings] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, ApiAppSettings]
}
