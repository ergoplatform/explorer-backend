package org.ergoplatform.explorer.settings

import cats.data.NonEmptyList
import org.ergoplatform.explorer.UrlString

final case class NetworkSettings(masterNodes: NonEmptyList[UrlString], selfCheckIntervalRequests: Int)
