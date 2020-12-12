package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import cats.effect.Sync
import org.ergoplatform.explorer.UrlString
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import org.ergoplatform.explorer.settings.pureConfigInstances._

import scala.concurrent.duration.FiniteDuration

final case class UtxTrackerSettings(
  pollInterval: FiniteDuration,
  masterNodesAddresses: NonEmptyList[UrlString],
  db: DbSettings,
  protocol: ProtocolSettings
)

object UtxTrackerSettings {

  def load[F[_]: Sync](pathOpt: Option[String]): F[UtxTrackerSettings] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, UtxTrackerSettings]
}
