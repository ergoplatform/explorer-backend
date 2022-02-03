package org.ergoplatform.explorer.http.api.v1.models

import org.ergoplatform.explorer.http.api.models.Items
import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}

@derive(encoder, decoder)
final case class OutputInfoM(filteredOutputs: Items[OutputInfo], mempoolOutputs: Items[UOutputInfo])

object OutputInfoM {

  implicit val schema: Schema[OutputInfoM] =
    Schema
      .derived[OutputInfoM]
      .modify(_.filteredOutputs)(_.description("Filtered boxes not spent in mempool"))
      .modify(_.mempoolOutputs)(_.description("Output boxes from mempool"))

  implicit val validator: Validator[OutputInfoM] = schema.validator
}
