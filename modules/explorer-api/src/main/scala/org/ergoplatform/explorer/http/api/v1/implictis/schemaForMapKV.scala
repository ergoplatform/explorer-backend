package org.ergoplatform.explorer.http.api.v1.implictis

import io.circe.KeyEncoder
import sttp.tapir.Schema

object schemaForMapKV {

  implicit def schemaForMap[K: KeyEncoder, V: Schema]: Schema[Map[K, V]] = {

    val schemaType =
      Schema
        .schemaForMap[V]
        .schemaType
        .contramap[Map[K, V]](_.map { case (key, value) =>
          (implicitly[KeyEncoder[K]].apply(key), value)
        }.toMap)

    Schema(schemaType)
  }
}
