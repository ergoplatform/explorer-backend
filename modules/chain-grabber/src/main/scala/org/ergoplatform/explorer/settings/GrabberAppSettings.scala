package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import cats.effect.Sync
import org.ergoplatform.explorer.UrlString
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import org.ergoplatform.explorer.settings.pureConfigInstances._

import scala.concurrent.duration.FiniteDuration

final case class GrabberAppSettings(
  chainPollInterval: FiniteDuration,
  utxPoolPollInterval: FiniteDuration,
  masterNodesAddresses: NonEmptyList[UrlString],
  dbSettings: DbSettings,
  protocol: ProtocolSettings
)

object GrabberAppSettings {

  def load[F[_]: Sync](pathOpt: Option[String]): F[GrabberAppSettings] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, GrabberAppSettings]
}
