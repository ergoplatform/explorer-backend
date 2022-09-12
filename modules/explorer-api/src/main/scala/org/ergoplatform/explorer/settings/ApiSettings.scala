package org.ergoplatform.explorer.settings

final case class ApiSettings(
  http: HttpSettings,
  db: DbSettings,
  protocol: ProtocolSettings,
  utxCache: UtxCacheSettings,
  redis: RedisSettings,
  service: ServiceSettings,
  requests: RequestsSettings,
  enableBroadcast: Boolean
)

object ApiSettings extends SettingCompanion[ApiSettings]
