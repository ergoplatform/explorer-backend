package org.ergoplatform.dex.settings

import org.ergoplatform.explorer.UrlString

final case class KafkaSettings(
  host: UrlString,
  port: Int,
  groupId: String,
  clientId: String,
  topicId: String
)
