package org.ergoplatform.explorer.http.api.settings

import org.ergoplatform.explorer.settings.{DbSettings, ProtocolSettings}

final case class Settings(
  httpSettings: HttpSettings,
  dbSettings: DbSettings,
  protocolSettings: ProtocolSettings
)
