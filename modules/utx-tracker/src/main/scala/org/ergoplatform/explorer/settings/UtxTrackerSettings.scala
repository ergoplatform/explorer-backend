package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import org.ergoplatform.explorer.UrlString

import scala.concurrent.duration.FiniteDuration

final case class UtxTrackerSettings(
  pollInterval: FiniteDuration,
  masterNodesAddresses: NonEmptyList[UrlString],
  db: DbSettings,
  protocol: ProtocolSettings
)

object UtxTrackerSettings extends SettingCompanion[UtxTrackerSettings]
