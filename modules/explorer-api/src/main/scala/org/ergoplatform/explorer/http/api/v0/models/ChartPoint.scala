package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.aggregates.TimePoint
import sttp.tapir.{Schema, Validator}
import org.ergoplatform.explorer.http.api.tapirInstances._

final case class ChartPoint(timestamp: Long, value: BigInt)

object ChartPoint {

  def apply(point: TimePoint[Long]): ChartPoint =
    ChartPoint(point.ts, BigInt(point.value))

  def fromBigIntPoint(point: TimePoint[BigInt]): ChartPoint =
    ChartPoint(point.ts, point.value)

  implicit val codec: Codec[ChartPoint] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[ChartPoint] = Schema.derived

  implicit val validator: Validator[ChartPoint] = schema.validator
}
