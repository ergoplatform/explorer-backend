package org.ergoplatform.explorer.settings

import cats.effect.Sync
import pureconfig.ConfigSource
import pureconfig.module.catseffect._
import pureconfig.generic.auto._

/** Database credentials and settings.
  */
final case class DbSettings(url: String, user: String, pass: String, cpSize: Int)

object DbSettings {

  def load[F[_]: Sync](pathOpt: Option[String]): F[DbSettings] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, DbSettings]
}
