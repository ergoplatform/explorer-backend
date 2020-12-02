package org.ergoplatform.explorer.http.api.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.{Schema, Validator}

final case class Items[A](items: List[A], total: Int)

object Items {

  implicit def codec[A: Codec]: Codec[Items[A]]             = deriveCodec
  implicit def schema[A: Schema]: Schema[Items[A]]          = Schema.derive[Items[A]]
  implicit def validator[A: Validator]: Validator[Items[A]] = Validator.derive[Items[A]]
}
