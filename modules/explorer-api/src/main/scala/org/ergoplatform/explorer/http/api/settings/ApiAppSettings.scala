package org.ergoplatform.explorer.http.api.settings

import cats.effect.Sync
import org.ergoplatform.explorer.settings.{DbSettings, ProtocolSettings}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import org.ergoplatform.explorer.settings.pureConfigInstances._

final case class ApiAppSettings(
  httpSettings: HttpSettings,
  dbSettings: DbSettings,
  protocol: ProtocolSettings
)

object ApiAppSettings {

  def load[F[_]: Sync](pathOpt: Option[String]): F[ApiAppSettings] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, ApiAppSettings]
}
