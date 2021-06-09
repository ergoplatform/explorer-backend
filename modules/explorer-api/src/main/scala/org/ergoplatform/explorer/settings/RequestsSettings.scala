package org.ergoplatform.explorer.settings

final case class RequestsSettings(
                                   maxEntitiesPerRequest: Int,
                                   maxEntitiesPerHeavyRequest: Int,
                                   maxBlocksPerRequest: Int
)
