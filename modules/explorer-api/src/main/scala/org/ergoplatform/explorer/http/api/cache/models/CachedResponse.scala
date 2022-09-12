package org.ergoplatform.explorer.http.api.cache.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.http4s.{Headers, HttpVersion, Status}
import tofu.logging.derivation.loggable

@derive(loggable, decoder, encoder)
case class CachedResponse(
                           status: Status,
                           httpVersion: HttpVersion,
                           headers: Headers,
                           body: String
                         )