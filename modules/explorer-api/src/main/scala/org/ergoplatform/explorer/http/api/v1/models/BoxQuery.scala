package org.ergoplatform.explorer.http.api.v1.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{KeyDecoder, KeyEncoder}
import org.ergoplatform.explorer.{ErgoTreeTemplateHash, RegisterId, TokenId}
import org.ergoplatform.explorer.RegisterId._
import sttp.tapir.codec.enumeratum._
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class BoxQuery(
  ergoTreeTemplateHash: ErgoTreeTemplateHash,
  registers: Option[Map[RegisterId, String]],
  constants: Option[Map[Int, String]],
  assets: Option[List[TokenId]]
)

object BoxQuery {

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

  implicit val schema: Schema[BoxQuery] =
    Schema
      .derived[BoxQuery]
      .modify(_.ergoTreeTemplateHash)(_.description("SHA-256 hash of ErgoTree template this box script should have"))
      .modify(_.registers)(_.description("Pairs of (register ID, register value) this box should contain"))
      .modify(_.constants)(_.description("Pairs of (constant index, constant value) this box should contain"))
      .modify(_.assets)(_.description("IDs of tokens returned boxes should contain"))

  implicit val validator: Validator[BoxQuery] = Validator.pass
}
