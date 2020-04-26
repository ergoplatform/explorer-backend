package org.ergoplatform.explorer.context

import cats.data.NonEmptyList
import cats.effect.Sync
import org.ergoplatform.explorer.UrlString
import org.ergoplatform.explorer.settings.pureConfigInstances._
import org.ergoplatform.explorer.settings.{DbSettings, ProtocolSettings}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import tofu.optics.macros.ClassyOptics

import scala.concurrent.duration.FiniteDuration

@ClassyOptics("contains_")
final case class SettingsContext(
  pollInterval: FiniteDuration,
  masterNodesAddresses: NonEmptyList[UrlString],
  db: DbSettings,
  protocol: ProtocolSettings
)

object SettingsContext {

  def make[F[_]: Sync](pathOpt: Option[String]): F[SettingsContext] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, SettingsContext]
}
