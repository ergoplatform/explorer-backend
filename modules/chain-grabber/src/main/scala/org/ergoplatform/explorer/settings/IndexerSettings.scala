package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import org.ergoplatform.explorer.UrlString
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

final case class IndexerSettings(
  pollInterval: FiniteDuration,
  writeOrphans: Boolean,
  masterNodesAddresses: NonEmptyList[UrlString],
  db: DbSettings,
  protocol: ProtocolSettings
)

object IndexerSettings extends SettingCompanion[IndexerSettings]
