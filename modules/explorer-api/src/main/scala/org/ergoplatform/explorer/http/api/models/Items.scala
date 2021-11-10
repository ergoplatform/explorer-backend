package org.ergoplatform.explorer.http.api.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class Items[A](items: List[A], total: Int)

object Items {

  implicit def schema[A: Schema]: Schema[Items[A]] =
    Schema.derived[Items[A]]
      .modify(_.items)(_.description("Items in selection"))
      .modify(_.total)(_.description("Total qty of items"))

  implicit def validator[A: Schema]: Validator[Items[A]] = schema.validator
}
