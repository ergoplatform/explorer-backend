package org.ergoplatform.explorer.http.api.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Items[A](items: List[A], total: Int)

object Items {

  implicit def codec[A: Codec]: Codec[Items[A]] = deriveCodec
}
