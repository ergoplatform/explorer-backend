package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import cats.effect.Sync
import org.ergoplatform.explorer.UrlString
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import org.ergoplatform.explorer.settings.pureConfigInstances._

import scala.concurrent.duration.FiniteDuration

final case class UtxBroadcasterSettings(
  masterNodesAddresses: NonEmptyList[UrlString],
  tickInterval: FiniteDuration,
  redis: RedisSettings
)

object UtxBroadcasterSettings {

  def load[F[_]: Sync](pathOpt: Option[String]): F[UtxBroadcasterSettings] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, UtxBroadcasterSettings]
}
