package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import org.ergoplatform.explorer.UrlString

import scala.concurrent.duration.FiniteDuration

final case class UtxBroadcasterSettings(
  masterNodesAddresses: NonEmptyList[UrlString],
  tickInterval: FiniteDuration,
  redis: RedisSettings,
  utxCache: UtxCacheSettings
)

object UtxBroadcasterSettings extends SettingCompanion[UtxBroadcasterSettings]
