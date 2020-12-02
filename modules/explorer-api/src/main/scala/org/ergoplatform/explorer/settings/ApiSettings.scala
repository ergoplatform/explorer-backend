package org.ergoplatform.explorer.settings

import cats.effect.Sync
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import org.ergoplatform.explorer.settings.pureConfigInstances._

final case class ApiSettings(
  http: HttpSettings,
  db: DbSettings,
  protocol: ProtocolSettings,
  utxCache: UtxCacheSettings,
  redis: RedisSettings,
  service: ServiceSettings,
  requests: RequestsSettings
)

object ApiSettings {

  def load[F[_]: Sync](pathOpt: Option[String]): F[ApiSettings] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, ApiSettings]
}
